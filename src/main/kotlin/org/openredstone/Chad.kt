package org.openredstone

import kotlin.system.exitProcess

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml
import org.javacord.api.DiscordApiBuilder
import org.openredstone.commands.*

import org.openredstone.entity.ChadSpec
import org.openredstone.listeners.*
import org.openredstone.managers.NotificationManager

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
            "Invalid number of arguments passed to command `$name`"
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

    println("Loading OREBot...")
    println("Notification channel ID: ${chadConfig.notificationChannelId}")
    println("Command character: \'${chadConfig.commandChar}\'")

    val discordApi = DiscordApiBuilder()
        .setToken(chadConfig.botToken)
        .login()
        .join()
        .apply {
            updateActivity(chadConfig.playingMessage)
        }

    val discordCommands = mapOf(
        "apply" to ApplyCommand,
        "roll" to RollCommand,
        "insult" to InsultCommand(chadConfig.insults),
        "authorized" to AuthorizedCommand(listOf("Staff"))
    ) + chadConfig.discordCommands.mapValues { StaticCommand(it.value) }
    val ircCommands = mapOf(
        "apply" to ApplyCommand,
        "insult" to InsultCommand(chadConfig.insults),
        "authorized" to AuthorizedCommand(listOf("op")),
        "list" to ListCommand(chadConfig.statusChannelId, discordApi)
    ) + chadConfig.ircCommands.mapValues { StaticCommand(it.value) }

    startDiscordCommandListener(discordApi, CommandExecutor(chadConfig.commandChar, discordCommands))
    startIRCCommandListener(chadConfig.irc, CommandExecutor(chadConfig.commandChar, ircCommands))

    if (chadConfig.disableSpoilers) {
        startSpoilerListener(discordApi)
    }

    NotificationManager(discordApi, chadConfig.notificationChannelId, chadConfig.notifications)
}
