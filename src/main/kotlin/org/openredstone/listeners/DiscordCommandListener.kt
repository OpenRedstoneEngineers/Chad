package org.openredstone.listeners

import org.javacord.api.DiscordApi
import org.javacord.api.event.message.MessageCreateEvent

import org.openredstone.commands.Commands
import org.openredstone.getAttemptedCommand
import org.openredstone.model.entity.ConfigEntity

class DiscordCommandListener(
    private val commands: Commands,
    private val discordApi: DiscordApi,
    private val config: ConfigEntity
) : Listener {
    override fun listen() {
        discordApi.addMessageCreateListener(this::messageCreated)
    }

    private fun messageCreated(event: MessageCreateEvent) {
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
}
