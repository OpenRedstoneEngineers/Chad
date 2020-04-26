package org.openredstone

import kotlin.system.exitProcess

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml
import com.uchuhimo.konf.toValue
import org.javacord.api.DiscordApiBuilder
import org.openredstone.commands.Commands
import org.openredstone.commands.StaticCommand
import org.openredstone.commands.ApplyCommand
import org.openredstone.commands.ErrorCommand
import org.openredstone.commands.RollCommand
import org.openredstone.commands.ListCommand
import org.openredstone.listeners.DiscordCommandListener
import org.openredstone.listeners.GeneralListener
import org.openredstone.listeners.IrcCommandListener
import org.openredstone.managers.NotificationManager
import org.openredstone.model.entity.ConfigEntity

data class AttemptedCommand(val reply: String, val privateReply: Boolean)

fun getAttemptedCommand(config: ConfigEntity, message: String, commands: Commands): AttemptedCommand? {
    if (message.isEmpty() || message[0] != config.commandChar) {
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

    val config = Config()
        .from.yaml.file(args[0])
        .at("chad")
        .toValue<ConfigEntity>()

    println("Loading OREBot...")
    println("Notification channel ID: ${config.notificationChannelId}")
    println("Command character: \'${config.commandChar}\'")

    val discordApi = DiscordApiBuilder()
        .setToken(config.botToken)
        .login()
        .join()
        .apply {
            updateActivity(config.playingMessage)
        }

    val discordCommands = mapOf(
        "apply" to ApplyCommand,
        "roll" to RollCommand
    ) + config.discordCommands.map { it.name to StaticCommand(it.reply) }
    val ircCommands = mapOf(
        "apply" to ApplyCommand,
        "list" to ListCommand(config.statusChannelId, discordApi)
    ) + config.ircCommands.map { it.name to StaticCommand(it.reply) }

    listOf(
        DiscordCommandListener(discordCommands, discordApi, config),
        IrcCommandListener(ircCommands, config)
    ).forEach { it.listen() }

    NotificationManager(
        discordApi,
        config.notificationChannelId,
        config.notifications
    ).apply {
        setupNotificationMessage()
        monitorNotifications()
    }

    if (config.disableSpoilers) {
        GeneralListener.startSpoilerListener(discordApi)
    }
}
