package org.openredstone.chad

import com.uchuhimo.konf.ConfigSpec

data class ChadConfig(
    val botToken: String,
    val databaseFile: String,
    val enableNotificationRoles: Boolean,
    val notificationChannelId: Long,
    val staffHelpChannelId: Long,
    val removedContentChannelId: Long,
    val welcomeChannelId: Long,
    val gameChatChannelId: Long,
    val statusChannelId: Long,
    val serverId: Long,
    val ingameBotRoleId: String,
    val playingMessage: String,
    val commandChar: Char,
    val disableSpoilers: Boolean,
    val notifications: List<NotificationRoleConfig>,
    val fractalDeets: FractalConfig,
    val authorizedDiscordRoles: List<String>,
    val logging: LoggingConfig,
    val botAutomod: AutomodConfig,
    val greetings: List<String>,
    val insults: List<String>,
    val protips: List<String>,
)

object ChadSpec : ConfigSpec("") {
    val chad by required<ChadConfig>()
}

data class AutomodConfig(val enableBotAutomod: Boolean, val automodChannelId: Long, val regexes: List<String>)

data class FractalConfig(val size: Int, val maxIterations: Int, val messiness: Int, val zoom: Double)

data class NotificationRoleConfig(val name: String, val role: String, val description: String)

data class LoggingConfig(val defaultLevel: String, val chadLevel: String, val dateTimeFormat: String)
