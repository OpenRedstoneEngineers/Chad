package org.openredstone.listeners

import org.openredstone.commands.CommandContext
import org.openredstone.managers.CommandManager
import org.openredstone.model.entity.ConfigEntity
import org.pircbotx.Configuration
import org.pircbotx.PircBotX
import org.pircbotx.hooks.ListenerAdapter
import org.pircbotx.hooks.events.MessageEvent
import kotlin.concurrent.thread

class IrcCommandListener(private var commandManager: CommandManager, private var config: ConfigEntity) : Listener() {
    class IrcListener(private var commandManager: CommandManager) : ListenerAdapter() {
        override fun onMessage(event: MessageEvent) {
            val command = commandManager.getAttemptedCommand(CommandContext.IRC, event.message) ?: return
            if (command.isPrivateMessageResponse) {
                event.user?.send()?.message(command.reply)
            } else {
                event.channel.send().message(command.reply)
            }
        }
    }

    override fun listen() {
        val ircBot = PircBotX(
            Configuration.Builder()
                .setName(config.irc.name)
                .addServer(config.irc.server)
                .addAutoJoinChannel(config.irc.channel)
                .setNickservPassword(config.irc.password)
                .addListener(IrcListener(commandManager))
                .setAutoReconnect(true)
                .buildConfiguration()
        )
        thread {
            ircBot.startBot()
        }
    }
}