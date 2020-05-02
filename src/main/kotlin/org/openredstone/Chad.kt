package org.openredstone

import kotlin.system.exitProcess

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml
import org.javacord.api.DiscordApiBuilder
import org.openredstone.commands.*

import org.openredstone.entity.ChadSpec
import org.openredstone.listeners.*
import org.openredstone.managers.NotificationManager

data class AttemptedCommand(val reply: String, val privateReply: Boolean)

fun getAttemptedCommand(sender:Sender, commandChar: Char, message: String, commands: Commands): AttemptedCommand? {
    if (message.isEmpty() || message[0] != commandChar) {
        return null
    }

    val args = message.split(" ")

    val name = parseCommandName(args)
    val executedCommand = commands[name] ?: ErrorCommand

    return if (args.size - 1 < executedCommand.requireParameters) {
        AttemptedCommand(
            "Invalid number of arguments passed to command `$name`",
            executedCommand.privateReply
        )
    } else {
        AttemptedCommand(
            executedCommand.runCommand(sender, args.drop(1)),
            executedCommand.privateReply
        )
    }
}

private fun parseCommandName(parts: List<String>) = parts[0].substring(1)

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
        "authorized" to AuthorizedCommand(listOf("Staff"))
    ) + chadConfig.discordCommands.mapValues { StaticCommand(it.value) }
    val ircCommands = mapOf(
        "apply" to ApplyCommand,
        "authorized" to AuthorizedCommand(listOf("op")),
        "list" to ListCommand(chadConfig.statusChannelId, discordApi)
    ) + chadConfig.ircCommands.mapValues { StaticCommand(it.value) }

    startDiscordCommandListener(discordCommands, discordApi, chadConfig.commandChar)
    startIRCCommandListener(ircCommands, chadConfig.irc, chadConfig.commandChar)

    if (chadConfig.disableSpoilers) {
        startSpoilerListener(discordApi)
    }

    NotificationManager(discordApi, chadConfig.notificationChannelId, chadConfig.notifications)
}
