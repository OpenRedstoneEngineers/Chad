package org.openredstone

import kotlin.system.exitProcess

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml
import com.uchuhimo.konf.source.yaml.toYaml
import mu.KotlinLogging
import org.javacord.api.DiscordApiBuilder

import org.openredstone.commands.*
import org.openredstone.entity.ChadSpec
import org.openredstone.listeners.startDiscordListeners
import org.openredstone.listeners.startIrcListeners
import org.openredstone.managers.NotificationManager

/**
 * The global logger for Chad.
 */
val logger = KotlinLogging.logger("Chad")

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Please specify a config file")
        exitProcess(1)
    }
    val configFile = args[0]

    val config = Config { addSpec(ChadSpec) }
        .from.yaml.file(configFile)

    val chadConfig = config[ChadSpec.chad]

    // Logging properties
    val loggingConfig = chadConfig.logging
    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", loggingConfig.defaultLevel)
    System.setProperty("org.slf4j.simpleLogger.log.Chad", loggingConfig.chadLevel)
    System.setProperty("org.slf4j.simpleLogger.log.IRC link listener", loggingConfig.chadLevel)
    System.setProperty("org.slf4j.simpleLogger.log.Spoiler listener", loggingConfig.chadLevel)
    System.setProperty("org.slf4j.simpleLogger.showDateTime", "true")
    System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", chadConfig.logging.dateTimeFormat)
    System.setProperty("org.slf4j.simpleLogger.showThreadName", "false")

    logger.info("Loading Chad...")
    logger.info("Notification channel ID: ${chadConfig.notificationChannelId}")
    logger.info("Command character: '${chadConfig.commandChar}'")
    logger.info("Disable spoilers: ${chadConfig.disableSpoilers}")
    logger.info("Link preview: ${chadConfig.enableLinkPreview}")

    val discordApi = DiscordApiBuilder()
        .setToken(chadConfig.botToken)
        .login()
        .join()
        .apply {
            updateActivity(chadConfig.playingMessage)
        }

    val commonCommands = concurrentMapOf<String, Command>()
    val discordCommands = concurrentMapOf<String, Command>()
    val ircCommands = concurrentMapOf<String, Command>()

    fun reloadCommands() {
        config.toYaml.toFile(configFile)
        val authorizedRoles = AuthorizedRoles(chadConfig.authorizedDiscordRoles, chadConfig.authorizedIrcRoles)

        logger.info("(Re)loading commands...")

        commonCommands.apply {
            clear()
            putAll(chadConfig.commonCommands.mapValues { staticCommand(it.value) })
            putAll(listOf(
                "add" to command(authorizedRoles) {
                    val name by required()
                    val message by vararg()
                    reply {
                        chadConfig.commonCommands[name] = message.joinToString(separator = " ")
                        reloadCommands()
                        "Done!"
                    }
                },
                "apply" to applyCommand,
                "authorized" to command(authorizedRoles) {
                    reply { "authorized !" }
                },
                "insult" to insultCommand(chadConfig.insults),
                "reload" to command(authorizedRoles) {
                    reply {
                        reloadCommands()
                        "Done!"
                    }
                }
            ))
        }

        discordCommands.apply {
            clear()
            put("roll", rollCommand)
            put("help", helpCommand(this))
            putAll(commonCommands)
            putAll(chadConfig.discordCommands.mapValues { staticCommand(it.value) })
        }

        ircCommands.apply {
            put("list", listCommand(chadConfig.statusChannelId, discordApi))
            put("help", helpCommand(this))
            putAll(commonCommands)
            putAll(chadConfig.ircCommands.mapValues { staticCommand(it.value) })
        }
    }

    reloadCommands()

    logger.info("Loaded the following Discord commands: ${discordCommands.keys.joinToString()}")
    logger.info("Loaded the following IRC commands: ${ircCommands.keys.joinToString()}")
    logger.info("Starting listeners...")

    startDiscordListeners(discordApi, CommandExecutor(chadConfig.commandChar, discordCommands), chadConfig.disableSpoilers)
    startIrcListeners(chadConfig.irc, CommandExecutor(chadConfig.commandChar, ircCommands), chadConfig.enableLinkPreview)

    if (chadConfig.enableNotificationRoles) NotificationManager(discordApi, chadConfig.notificationChannelId, chadConfig.notifications)

    logger.info("Started listeners")
}
