package org.openredstone.listeners

import org.openredstone.managers.CommandManager
import org.openredstone.model.context.CommandContext
import org.pircbotx.hooks.ListenerAdapter
import org.pircbotx.hooks.events.MessageEvent

class IrcCommandListener(var commandManager: CommandManager) : ListenerAdapter() {

    override fun onMessage(event: MessageEvent?) {
        event?.message?.let {
            commandManager.getAttemptedCommand(CommandContext.IRC, it).ifPresent { command ->
                if (command.isPrivateMessageResponse) {
                    event.user?.let { user ->
                        user.send().message(command.reply)
                    }
                } else {
                    event.channel.send().message(command.reply)
                }
            }
        }
    }

}