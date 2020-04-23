package org.openredstone.listeners

import org.javacord.api.DiscordApi
import org.openredstone.managers.CommandManager
import org.openredstone.commands.CommandContext
import org.pircbotx.hooks.ListenerAdapter
import org.pircbotx.hooks.events.MessageEvent

object GeneralListener {
    fun startSpoilerListener(discordApi: DiscordApi) {
        discordApi.addMessageCreateListener { event ->
            if (event.message.content.contains(Regex("\\|\\|"))) {
                event.message.delete()
            }
        }
    }
}

class IrcCommandListener(private var commandManager: CommandManager) : ListenerAdapter() {
    override fun onMessage(event: MessageEvent?) {
        event?.message?.let {
            commandManager.getAttemptedCommand(CommandContext.IRC, it)?.let { command ->
                if (command.isPrivateMessageResponse) {
                    event.user?.send()?.message(command.reply)
                } else {
                    event.channel.send().message(command.reply)
                }
            }
        }
    }
}
