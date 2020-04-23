package org.openredstone

import kotlin.system.exitProcess

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml
import com.uchuhimo.konf.toValue
import org.javacord.api.DiscordApiBuilder
import org.openredstone.commands.both.ApplyCommand
import org.openredstone.commands.discord.RollCommand
import org.openredstone.commands.irc.ListCommand
import org.openredstone.listeners.GeneralListener
import org.openredstone.managers.CommandManager
import org.openredstone.managers.NotificationManager
import org.openredstone.model.entity.ConfigEntity

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

    val commandManager = CommandManager(discordApi, config).apply {
        addCommands(
            ApplyCommand(),
            RollCommand(),
            ListCommand(config.statusChannelId, discordApi)
        )
        addStaticCommands(config.commands)
        startListeners()
    }

    val notificationManager = NotificationManager(
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