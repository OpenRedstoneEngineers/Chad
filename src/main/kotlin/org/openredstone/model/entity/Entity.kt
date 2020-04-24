package org.openredstone.model.entity

data class CommandEntity(val name: String, val reply: String, val context: String)

data class ConfigEntity(
    val botToken: String,
    val notificationChannelId: Long,
    val statusChannelId: Long,
    val commandChar: Char,
    val disableSpoilers: Boolean,
    val irc: IrcBotEntity,
    val commands: List<CommandEntity>,
    val notifications: List<NotificationRoleEntity>
)

data class IrcBotEntity(val name: String, val server: String, val channel: String, val password: String)

data class NotificationRoleEntity(val emoji: String, val name: String, val role: String, val description: String)
