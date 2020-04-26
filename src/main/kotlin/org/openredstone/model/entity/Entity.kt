package org.openredstone.model.entity

data class ConfigEntity(
    val botToken: String,
    val notificationChannelId: Long,
    val statusChannelId: Long,
    val playingMessage: String,
    val commandChar: Char,
    val disableSpoilers: Boolean,
    val irc: IrcBotEntity,
    val notifications: List<NotificationRoleEntity>,
    val discordCommands: Map<String, String>,
    val ircCommands: Map<String, String>
)

data class IrcBotEntity(val name: String, val server: String, val channel: String, val password: String)

data class NotificationRoleEntity(val emoji: String, val name: String, val role: String, val description: String)
