package org.openredstone.listeners

import org.javacord.api.DiscordApi
import org.javacord.api.event.message.MessageCreateEvent

import org.openredstone.commands.Commands
import org.openredstone.getAttemptedCommand
import org.openredstone.entity.ConfigEntity

fun startDiscordCommandListener(commands: Commands, discordApi: DiscordApi, config: ConfigEntity) {
    fun messageCreated(event: MessageCreateEvent) {
        val user = event.messageAuthor.asUser().get()
        if (user.isBot) {
            return
        }
        val command = getAttemptedCommand(config, event.messageContent, commands) ?: return
        if (command.privateReply) {
            user.sendMessage(command.reply)
        } else {
            event.channel.sendMessage(command.reply)
        }
    }
    discordApi.addMessageCreateListener(::messageCreated)
}
