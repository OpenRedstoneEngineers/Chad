package org.openredstone.listeners

import org.javacord.api.DiscordApi
import org.javacord.api.entity.permission.Role
import org.javacord.api.event.message.MessageCreateEvent

import org.openredstone.commands.Commands
import org.openredstone.commands.Sender
import org.openredstone.commands.Service
import org.openredstone.getAttemptedCommand

fun startDiscordCommandListener(commands: Commands, discordApi: DiscordApi, commandChar: Char) {
    fun messageCreated(event: MessageCreateEvent) {
        val user = event.messageAuthor.asUser().get()
        if (user.isBot) {
            return
        }
        val roles = user.getRoles(event.server.get()).map(Role::getName)
        val sender = Sender(Service.DISCORD, user.getDisplayName(event.server.get()), roles)
        val command = getAttemptedCommand(sender, commandChar, event.messageContent, commands) ?: return
        if (command.privateReply) {
            user.sendMessage(command.reply)
        } else {
            event.channel.sendMessage(command.reply)
        }
    }
    discordApi.addMessageCreateListener(::messageCreated)
}

fun startSpoilerListener(discordApi: DiscordApi) {
    discordApi.addMessageCreateListener { event ->
        if (event.message.content.contains(Regex("\\|\\|"))) {
            event.message.delete()
        }
    }
}
