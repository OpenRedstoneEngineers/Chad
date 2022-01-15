package org.openredstone.entity

import com.uchuhimo.konf.ConfigSpec

data class ChadConfig(
    val botToken: String,
    val databaseFile: String,
    val enableNotificationRoles: Boolean,
    val notificationChannelId: Long,
    val welcomeChannelId: Long,
    val gameChatChannelId: Long,
    val statusChannelId: Long,
    val serverId: Long,
    val ingameBotRoleId: String,
    val playingMessage: String,
    val commandChar: Char,
    val disableSpoilers: Boolean,
    val notifications: List<NotificationRoleConfig>,
    val authorizedDiscordRoles: List<String>,
    val logging: LoggingConfig,
    val greetings: List<String>,
    val insults: List<String>,
    val discordCommands: Map<String, String>,
)

object ChadSpec : ConfigSpec("") {
    val chad by required<ChadConfig>()
}

data class NotificationRoleConfig(val emoji: String, val name: String, val role: String, val description: String)

data class LoggingConfig(val defaultLevel: String, val chadLevel: String, val dateTimeFormat: String)
