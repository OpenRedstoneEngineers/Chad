package org.openredstone

import kotlin.system.exitProcess

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml
import org.javacord.api.DiscordApiBuilder

import org.openredstone.commands.*
import org.openredstone.entity.ChadSpec
import org.openredstone.listeners.startDiscordListeners
import org.openredstone.listeners.startIrcListeners
import org.openredstone.managers.NotificationManager

typealias Commands = Map<String, Command>

data class CommandResponse(val reply: String, val privateReply: Boolean)

class CommandExecutor(private val commandChar: Char, private val commands: Commands) {
    fun tryExecute(sender: Sender, message: String): CommandResponse? {
        if (message.isEmpty() || message[0] != commandChar) {
            return null
        }

        val parts = message.split(" ")
        val args = parts.drop(1)
        val name = parts[0].substring(1)
        val command = commands[name] ?: ErrorCommand

        val reply = if (args.size < command.requireParameters) {
            "Not enough arguments passed to command `$name`, expected at least ${command.requireParameters}."
        } else {
            command.runCommand(sender, args)
        }
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

    println("Loading Chad...")
    println("Notification channel ID: ${chadConfig.notificationChannelId}")
    println("Command character: \'${chadConfig.commandChar}\'")

    val discordApi = DiscordApiBuilder()
        .setToken(chadConfig.botToken)
        .login()
        .join()
        .apply {
            updateActivity(chadConfig.playingMessage)
        }

    val commonCommands = chadConfig.commonCommands.mapValues { staticCommand(it.value) }
    val discordCommands = mapOf(
        "apply" to applyCommand,
        "authorized" to AuthorizedCommand(chadConfig.authorizedDiscordRoles),
        "help" to helpCommand,
        "insult" to insultCommand(chadConfig.insults),
        "roll" to rollCommand
    ) + commonCommands + chadConfig.discordCommands.mapValues { staticCommand(it.value) }
    val ircCommands = mapOf(
        "apply" to applyCommand,
        "authorized" to AuthorizedCommand(chadConfig.authorizedIrcRoles),
        "help" to helpCommand,
        "insult" to insultCommand(chadConfig.insults),
        "list" to listCommand(chadConfig.statusChannelId, discordApi)
    ) + commonCommands + chadConfig.ircCommands.mapValues { staticCommand(it.value) }

    startDiscordListeners(discordApi, CommandExecutor(chadConfig.commandChar, discordCommands), chadConfig.disableSpoilers)
    startIrcListeners(chadConfig.irc, CommandExecutor(chadConfig.commandChar, ircCommands), chadConfig.enableLinkPreview)

    NotificationManager(discordApi, chadConfig.notificationChannelId, chadConfig.notifications)
}
