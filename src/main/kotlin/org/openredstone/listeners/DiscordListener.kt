package org.openredstone.listeners

import mu.KotlinLogging
import org.javacord.api.DiscordApi
import org.javacord.api.entity.permission.Role
import org.javacord.api.event.message.MessageCreateEvent

import org.openredstone.commands.CommandExecutor
import org.openredstone.commands.Sender
import org.openredstone.commands.Service
import org.openredstone.toNullable

private fun startDiscordCommandListener(discordApi: DiscordApi, gameChatChannelId: Long, executor: CommandExecutor) {
    fun messageCreated(event: MessageCreateEvent) {
        if (event.channel.id == gameChatChannelId) {
            return
        }
        val user = event.messageAuthor.asUser().toNullable() ?: return
        if (user.isBot) {
            return
        }
        val server = event.server.get()
        val roles = user.getRoles(server).map(Role::getName)
        val username = user.getDisplayName(server)
        val sender = Sender(Service.DISCORD, username, roles)
        val response = executor.tryExecute(sender, event.messageContent) ?: return
        if (response.privateReply) {
            user.sendMessage(response.reply)
        } else {
            event.channel.sendMessage("$username ${response.reply}")
        }
    }
    discordApi.addMessageCreateListener(::messageCreated)
}

val spoilerLogger = KotlinLogging.logger("Spoiler listener")

private fun startSpoilerListener(discordApi: DiscordApi) {
    discordApi.addMessageCreateListener { event ->
        val message = event.message
        if (message.content.contains(Regex("\\|\\|"))) {
            spoilerLogger.debug("${message.author} [${message.channel}]: ${message.content}")

            message.delete()
        }
    }
}

fun startDiscordListeners(discordApi: DiscordApi, gameChatChannelId: Long, executor: CommandExecutor, disableSpoilers: Boolean) {
    startDiscordCommandListener(discordApi, gameChatChannelId, executor)
    if (disableSpoilers) startSpoilerListener(discordApi)
}
