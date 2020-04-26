package org.openredstone.listeners

import org.openredstone.commands.Commands
import org.openredstone.entity.ConfigEntity
import org.openredstone.getAttemptedCommand
import org.pircbotx.Configuration
import org.pircbotx.PircBotX
import org.pircbotx.hooks.ListenerAdapter
import org.pircbotx.hooks.events.MessageEvent
import kotlin.concurrent.thread

private class IrcListener(private val commands: Commands, private val config: ConfigEntity) : ListenerAdapter() {
    override fun onMessage(event: MessageEvent) {
        val command = getAttemptedCommand(config, event.message, commands) ?: return
        if (command.privateReply) {
            event.user?.send()?.message(command.reply)
        } else {
            event.channel.send().message(command.reply)
        }
    }
}

fun startIRCCommandListener(commands: Commands, config: ConfigEntity) {
    val ircBot = PircBotX(
        Configuration.Builder()
            .setName(config.irc.name)
            .addServer(config.irc.server)
            .addAutoJoinChannel(config.irc.channel)
            .setNickservPassword(config.irc.password)
            .addListener(IrcListener(commands, config))
            .setAutoReconnect(true)
            .buildConfiguration()
    )
    thread {
        ircBot.startBot()
    }
}
