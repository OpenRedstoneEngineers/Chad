package org.openredstone.listeners

import org.openredstone.commands.CommandContext
import org.openredstone.managers.CommandManager
import org.openredstone.model.entity.ConfigEntity

class DiscordCommandListener(private var commandManager: CommandManager, private var config: ConfigEntity) : Listener() {
    override fun listen() {
        commandManager.discordApi.addMessageCreateListener { event ->
            if (!event.messageAuthor.asUser().get().isBot) {
                commandManager.getAttemptedCommand(CommandContext.DISCORD, event.messageContent)?.let { command ->
                    if (command.isPrivateMessageResponse) {
                        event.messageAuthor.asUser().get().sendMessage(command.reply)
                    } else {
                        event.channel.sendMessage(command.reply)
                    }
                }
            }
        }
    }
}