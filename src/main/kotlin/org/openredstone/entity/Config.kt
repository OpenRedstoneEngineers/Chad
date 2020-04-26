package org.openredstone.entity

import com.uchuhimo.konf.ConfigSpec

data class ChadConfig (
    val botToken: String,
    val notificationChannelId: Long,
    val statusChannelId: Long,
    val playingMessage: String,
    val commandChar: Char,
    val disableSpoilers: Boolean,
    val irc: IrcBotConfig,
    val notifications: List<NotificationRoleConfig>,
    val discordCommands: Map<String, String>,
    val ircCommands: Map<String, String>
)

object ChadSpec : ConfigSpec("") {
    val chad by required<ChadConfig>()
}

data class IrcBotConfig(val name: String, val server: String, val channel: String, val password: String)

data class NotificationRoleConfig(val emoji: String, val name: String, val role: String, val description: String)
