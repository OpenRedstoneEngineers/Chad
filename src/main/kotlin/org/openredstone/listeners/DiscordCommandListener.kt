package org.openredstone.listeners

import org.javacord.api.event.message.MessageCreateEvent
import org.openredstone.commands.CommandContext
import org.openredstone.managers.CommandManager

class DiscordCommandListener(private var commandManager: CommandManager) : Listener() {
    override fun listen() {
        commandManager.discordApi.addMessageCreateListener(this::messageCreated)
    }

    private fun messageCreated(event: MessageCreateEvent) {
        val user = event.messageAuthor.asUser().get()
        if (user.isBot) {
            return
        }
        val command = commandManager.getAttemptedCommand(CommandContext.DISCORD, event.messageContent) ?: return
        if (command.isPrivateMessageResponse) {
            user.sendMessage(command.reply)
        } else {
            event.channel.sendMessage(command.reply)
        }
    }
}