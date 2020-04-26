package org.openredstone.listeners

import kotlin.concurrent.thread

import org.pircbotx.Configuration
import org.pircbotx.PircBotX
import org.pircbotx.hooks.ListenerAdapter
import org.pircbotx.hooks.events.MessageEvent

import org.openredstone.commands.Commands
import org.openredstone.getAttemptedCommand
import org.openredstone.entity.IrcBotConfig

private class IrcListener(private val commands: Commands, private val commandChar: Char) : ListenerAdapter() {
    override fun onMessage(event: MessageEvent) {
        val command = getAttemptedCommand(commandChar, event.message, commands) ?: return
        if (command.privateReply) {
            event.user?.send()?.message(command.reply)
        } else {
            event.channel.send().message(command.reply)
        }
    }
}

fun startIRCCommandListener(commands: Commands, ircConfig: IrcBotConfig, commandChar: Char) {
    val ircBot = PircBotX(
        Configuration.Builder()
            .setName(ircConfig.name)
            .addServer(ircConfig.server)
            .addAutoJoinChannel(ircConfig.channel)
            .setNickservPassword(ircConfig.password)
            .addListener(IrcListener(commands, commandChar))
            .setAutoReconnect(true)
            .buildConfiguration()
    )
    thread {
        ircBot.startBot()
    }
}
