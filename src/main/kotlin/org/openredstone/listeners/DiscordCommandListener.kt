package org.openredstone.listeners

import org.javacord.api.DiscordApi
import org.javacord.api.event.message.MessageCreateEvent

import org.openredstone.commands.Commands
import org.openredstone.getAttemptedCommand

fun startDiscordCommandListener(commands: Commands, discordApi: DiscordApi, commandChar: Char) {
    fun messageCreated(event: MessageCreateEvent) {
        val user = event.messageAuthor.asUser().get()
        if (user.isBot) {
            return
        }
        val command = getAttemptedCommand(commandChar, event.messageContent, commands) ?: return
        if (command.privateReply) {
            user.sendMessage(command.reply)
        } else {
            event.channel.sendMessage(command.reply)
        }
    }
    discordApi.addMessageCreateListener(::messageCreated)
}
