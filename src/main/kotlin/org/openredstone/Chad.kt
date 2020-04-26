package org.openredstone

import kotlin.system.exitProcess

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml
import org.javacord.api.DiscordApiBuilder
import org.openredstone.commands.Commands
import org.openredstone.commands.StaticCommand
import org.openredstone.commands.ApplyCommand
import org.openredstone.commands.ErrorCommand
import org.openredstone.commands.RollCommand
import org.openredstone.commands.ListCommand
import org.openredstone.entity.ChadConfig
import org.openredstone.managers.NotificationManager
import org.openredstone.listeners.*

data class AttemptedCommand(val reply: String, val privateReply: Boolean)

fun getAttemptedCommand(commandChar: Char, message: String, commands: Commands): AttemptedCommand? {
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
            executedCommand.runCommand(args.drop(1)),
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

    val config = Config { addSpec(ChadConfig) }
        .from.yaml.file(args[0])

    println("Loading OREBot...")
    println("Notification channel ID: ${config[ChadConfig.notificationChannelId]}")
    println("Command character: \'${config[ChadConfig.commandChar]}\'")

    val discordApi = DiscordApiBuilder()
        .setToken(config[ChadConfig.botToken])
        .login()
        .join()
        .apply {
            updateActivity(config[ChadConfig.playingMessage])
        }

    val discordCommands = mapOf(
        "apply" to ApplyCommand,
        "roll" to RollCommand
    ) + config[ChadConfig.discordCommands].mapValues { StaticCommand(it.value) }
    val ircCommands = mapOf(
        "apply" to ApplyCommand,
        "list" to ListCommand(config[ChadConfig.statusChannelId], discordApi)
    ) + config[ChadConfig.ircCommands].mapValues { StaticCommand(it.value) }

    startDiscordCommandListener(discordCommands, discordApi, config[ChadConfig.commandChar])
    startIRCCommandListener(ircCommands, config[ChadConfig.irc], config[ChadConfig.commandChar])

    if (config[ChadConfig.disableSpoilers]) {
        startSpoilerListener(discordApi)
    }

    NotificationManager(
        discordApi,
        config[ChadConfig.notificationChannelId],
        config[ChadConfig.notifications]
    ).apply {
        setupNotificationMessage()
        monitorNotifications()
    }
}
