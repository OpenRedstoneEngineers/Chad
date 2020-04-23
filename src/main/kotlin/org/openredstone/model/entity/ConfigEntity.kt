package org.openredstone.model.entity

data class ConfigEntity (val botToken: String, val notificationChannelId: Long, val statusChannelId: Long, val commandChar: Char, val disableSpoilers: Boolean, val irc: IrcBotEntity, val commands: List<CommandEntity>, val notifications: List<NotificationRoleEntity>)