package org.openredstone

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml
import com.uchuhimo.konf.toValue
import org.javacord.api.DiscordApiBuilder
import org.openredstone.commands.StaticCommand
import org.openredstone.commands.both.ApplyCommand
import org.openredstone.commands.discord.RollCommand
import org.openredstone.commands.irc.ListCommand
import org.openredstone.listeners.DiscordCommandListener
import org.openredstone.listeners.GeneralListener
import org.openredstone.listeners.IrcCommandListener
import org.openredstone.managers.CommandManager
import org.openredstone.managers.NotificationManager
import org.openredstone.model.entity.ConfigEntity
import kotlin.system.exitProcess

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

    val commands = listOf(
        ApplyCommand,
        RollCommand,
        ListCommand(config.statusChannelId, discordApi)
    ) + config.commands.map { StaticCommand(it.context, it.name, it.reply) }

    val commandManager = CommandManager(config, commands)

    listOf(
        DiscordCommandListener(commandManager, discordApi),
        IrcCommandListener(commandManager, config)
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