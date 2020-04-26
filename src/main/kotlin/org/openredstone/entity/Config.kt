package org.openredstone.entity

import com.uchuhimo.konf.ConfigSpec

object ChadConfig : ConfigSpec("chad") {
    val botToken by required<String>()
    val notificationChannelId by required<Long>()
    val statusChannelId by required<Long>()
    val playingMessage by required<String>()
    val commandChar by required<Char>()
    val disableSpoilers by required<Boolean>()
    val irc by required<IrcBotConfig>()
    val notifications by required<List<NotificationRoleConfig>>()
    val discordCommands by required<Map<String, String>>()
    val ircCommands by required<Map<String, String>>()
}

data class IrcBotConfig(val name: String, val server: String, val channel: String, val password: String)

data class NotificationRoleConfig(val emoji: String, val name: String, val role: String, val description: String)
