package org.openredstone.listeners

import org.javacord.api.DiscordApi
import org.javacord.api.entity.permission.Role
import org.javacord.api.event.message.MessageCreateEvent
import org.openredstone.CommandExecutor
import org.openredstone.commands.Sender
import org.openredstone.commands.Service

fun startDiscordCommandListener(discordApi: DiscordApi, executor: CommandExecutor) {
    fun messageCreated(event: MessageCreateEvent) {
        val user = event.messageAuthor.asUser().get()
        if (user.isBot) {
            return
        }
        val server = event.server.get()
        val roles = user.getRoles(server).map(Role::getName)
        val sender = Sender(Service.DISCORD, user.getDisplayName(server), roles)
        val response = executor.tryExecute(sender, event.messageContent) ?: return
        if (response.privateReply) {
            user.sendMessage(response.reply)
        } else {
            event.channel.sendMessage(response.reply)
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
