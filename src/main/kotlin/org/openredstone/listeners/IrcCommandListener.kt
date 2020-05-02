package org.openredstone.listeners

import org.openredstone.commands.Commands
import org.openredstone.commands.Sender
import org.openredstone.commands.Service
import org.openredstone.entity.IrcBotConfig
import org.openredstone.getAttemptedCommand
import org.pircbotx.Configuration
import org.pircbotx.PircBotX
import org.pircbotx.hooks.ListenerAdapter
import org.pircbotx.hooks.events.MessageEvent
import kotlin.concurrent.thread

class IrcListener(val ircConfig: IrcBotConfig, val commands: Commands, private val commandChar: Char) : ListenerAdapter() {
    override fun onMessage(event: MessageEvent) {
        val role = if (event.user?.channelsOpIn!!.any { ircConfig.channel == it.name })
            "op" else ""
        val sender = Sender(Service.IRC, event.user?.nick.toString(), listOf(role))
        val command = getAttemptedCommand(sender, commandChar, event.message, commands) ?: return
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
            .addListener(IrcListener(ircConfig, commands, commandChar))
            .setAutoReconnect(true)
            .buildConfiguration()
    )
    thread {
        ircBot.startBot()
    }
}
