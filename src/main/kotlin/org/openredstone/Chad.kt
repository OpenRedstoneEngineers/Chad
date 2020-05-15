package org.openredstone

import kotlin.system.exitProcess

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml
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

typealias Commands = Map<String, Command>

data class CommandResponse(val reply: String, val privateReply: Boolean)

class CommandExecutor(private val commandChar: Char, private val commands: Commands) {
    fun tryExecute(sender: Sender, message: String): CommandResponse? {
        if (message.isEmpty() || message[0] != commandChar) {
            return null
        }

        logger.info("${sender.username} [${sender.service}]: $message")

        val parts = message.split(" ")
        val args = parts.drop(1)
        val name = parts[0].substring(1)
        val command = commands[name] ?: invalidCommand

        val reply = if (args.size < command.requireParameters) {
            "Not enough arguments passed to command `$name`, expected at least ${command.requireParameters}."
        } else {
            try {
                "${sender.username}: " + command.runCommand(sender, args)
            } catch (e: Exception) {
                logger.error(e) { "caught exception while running command" }

                "An error occurred while running the command."
            }
        }

        logger.debug("Reply to ${sender.username} [${sender.service}]: $reply")

        return CommandResponse(reply, command.privateReply)
    }
}

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Please specify a config file")
        exitProcess(1)
    }

    val config = Config { addSpec(ChadSpec) }
        .from.yaml.file(args[0])

    val chadConfig = config[ChadSpec.chad]

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

    val commonCommands = chadConfig.commonCommands.mapValues { staticCommand(it.value) }
    val discordCommands = mutableMapOf(
        "apply" to applyCommand,
        "authorized" to AuthorizedCommand(chadConfig.authorizedDiscordRoles),
        "insult" to insultCommand(chadConfig.insults),
        "roll" to rollCommand
    ).apply {
        putAll(commonCommands)
        putAll(chadConfig.discordCommands.mapValues { staticCommand(it.value) })
        put("help", helpCommand(this))
    }

    val ircCommands = mutableMapOf(
        "apply" to applyCommand,
        "authorized" to AuthorizedCommand(chadConfig.authorizedIrcRoles),
        "insult" to insultCommand(chadConfig.insults),
        "list" to listCommand(chadConfig.statusChannelId, discordApi)
    ).apply {
        putAll(commonCommands)
        putAll(chadConfig.ircCommands.mapValues { staticCommand(it.value) })
        put("help", helpCommand(this))
    }

    logger.info("Loaded the following Discord commands: ${discordCommands.keys.joinToString()}")
    logger.info("Loaded the following IRC commands: ${ircCommands.keys.joinToString()}")
    logger.info("Starting listeners...")

    startDiscordListeners(discordApi, CommandExecutor(chadConfig.commandChar, discordCommands), chadConfig.disableSpoilers)
    startIrcListeners(chadConfig.irc, CommandExecutor(chadConfig.commandChar, ircCommands), chadConfig.enableLinkPreview)

    if (chadConfig.enableNotificationRoles) NotificationManager(discordApi, chadConfig.notificationChannelId, chadConfig.notifications)

    logger.info("Started listeners")
}
