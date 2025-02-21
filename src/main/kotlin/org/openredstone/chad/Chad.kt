package org.openredstone.chad

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import mu.KotlinLogging
import org.javacord.api.DiscordApiBuilder
import org.openredstone.chad.commands.*
import org.openredstone.chad.commands.dsl.command
import kotlin.NoSuchElementException
import kotlin.system.exitProcess

/**
 * The global logger for Chad.
 */
val logger = KotlinLogging.logger("Chad")

/**
 * The main function.
 */
fun main(args: Array<String>) = runBlocking {
    // argument parsing
    if (args.size != 1) {
        eprintln("Expected one argument, got ${args.size}")
        eprintln("Usage: Chad config")
        exitProcess(1)
    }
    val configFile = args[0]

    // loading config
    val config = Config { addSpec(ChadSpec) }.from.yaml.watchFile(configFile)
    var chadConfig = config[ChadSpec.chad]
    val database = Sql(chadConfig.databaseFile)
    database.initTables()

    // logging properties
    val loggingConfig = chadConfig.logging
    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", loggingConfig.defaultLevel)
    System.setProperty("org.slf4j.simpleLogger.log.Chad", loggingConfig.chadLevel)
    System.setProperty("org.slf4j.simpleLogger.log.Spoiler listener", loggingConfig.chadLevel)
    System.setProperty("org.slf4j.simpleLogger.showDateTime", "true")
    System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", chadConfig.logging.dateTimeFormat)
    System.setProperty("org.slf4j.simpleLogger.showThreadName", "false")

    logger.info("Loading Chad...")
    logger.info("Notification channel ID: ${chadConfig.notificationChannelId}")
    logger.info("Command character: '${chadConfig.commandChar}'")
    logger.info("Disable spoilers: ${chadConfig.disableSpoilers}")

    val discordApi = DiscordApiBuilder()
        .setToken(chadConfig.botToken)
        .setAllIntents()
        .login()
        .await()
        .apply { updateActivity(chadConfig.playingMessage) }

    val discordServer = discordApi.getServerById(chadConfig.serverId)
        .orElseThrow { NoSuchElementException("Server not found") }

    val commands = concurrentMapOf<String, Command>()

    fun regenerateHelpCommand() {
        commands["help"] = helpCommand(commands)
    }

    fun reloadCommands() {
        chadConfig = config[ChadSpec.chad]
        val authorizedRoles = chadConfig.authorizedDiscordRoles

        logger.info("(Re)loading commands...")

        commands.apply {
            clear()
            put("conv", convertCommand)
            val bases = listOf("b" to 2, "o" to 8, "d" to 10, "h" to 16)
            bases.forEach { (oldName, oldBase) ->
                bases.forEach { (newName, newBase) ->
                    put("${oldName}2$newName", shortConvertCommand(oldBase, newBase))
                }
            }
            put("poll", pollCommand)
            put("rng", rngCommand)
            put("roll", rollCommand)
            put("lmgtfy", lmgtfy)
            put("apply", applyCommand)
            put("uuid", uuidCommand)
            put("insult", insultCommand(chadConfig.insults))
            put("protip", protipCommand(chadConfig.protips))
            put("add", command(authorizedRoles) {
                val name by required()
                val messages by vararg()
                reply {
                    val msg = messages.joinToString(separator = " ")
                    val cmd = command {
                        reply { msg }
                    }
                    database.insertCommand(name, msg)
                    commands[name] = cmd
                    regenerateHelpCommand()
                    "Done!"
                }
            })
            put("remove", command(authorizedRoles) {
                val name by required()
                reply {
                    database.removeCommand(name)
                    commands.remove(name)
                    regenerateHelpCommand()
                    "Done!"
                }
            })
            put("issue", issueCommand(authorizedRoles, discordApi, chadConfig, discordServer))
            put("delete", deleteCommand(authorizedRoles, discordApi, chadConfig))
            put("pikl", piklCommand(authorizedRoles, discordServer, discordApi))
            put("trust", trustCommand(authorizedRoles, discordServer, discordApi))
            put("authorized", command(authorizedRoles) {
                reply { "authorized !" }
            })
            put("reload", command(authorizedRoles) {
                reply {
                    reloadCommands()
                    "Done!"
                }
            })
            put("history", historyCommand(authorizedRoles, database))
            put("fractal", fractalCommand(authorizedRoles, chadConfig))
            putAll(database.getCommands().mapValues { staticCommand(it.value) })
            put("help", helpCommand(this))
        }
    }

    reloadCommands()

    logger.info("Loaded the following Discord commands: ${commands.keys.joinToString()}")
    logger.info("Starting listeners...")

    startDiscordListeners(
        logger,
        discordApi,
        CommandExecutor(chadConfig.commandChar, commands),
        chadConfig.disableSpoilers,
        chadConfig.botAutomod.enableBotAutomod,
        chadConfig.botAutomod.automodChannelId,
        chadConfig.botAutomod.regexes,
        chadConfig.welcomeChannelId,
        chadConfig.greetings,
        chadConfig.ingameBotRoleId,
        chadConfig.gameChatChannelId,
        database,
        this
    )

    if (chadConfig.enableNotificationRoles) NotificationManager(
        discordApi,
        chadConfig.notificationChannelId,
        chadConfig.notifications,
    )

    logger.info("Done")

    // since javacord does it's own thing, need to prevent this coroutine from exiting...
    while (isActive) {
        // just sleep
        delay(1000)
    }
}
