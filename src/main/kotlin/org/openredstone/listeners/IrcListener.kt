package org.openredstone.listeners

import org.jsoup.Jsoup
import org.openredstone.CommandExecutor
import org.openredstone.commands.Sender
import org.openredstone.commands.Service
import org.openredstone.entity.IrcBotConfig
import org.pircbotx.Configuration
import org.pircbotx.PircBotX
import org.pircbotx.hooks.ListenerAdapter
import org.pircbotx.hooks.events.MessageEvent
import java.net.UnknownHostException
import kotlin.concurrent.thread

class IrcCommandListener(private val ircConfig: IrcBotConfig, private val executor: CommandExecutor) : ListenerAdapter() {
    override fun onMessage(event: MessageEvent) {
        val role = if (event.user?.channelsOpIn!!.any { ircConfig.channel == it.name })
            "op" else ""
        val sender = Sender(Service.IRC, event.user?.nick.toString(), listOf(role))
        val command = executor.tryExecute(sender, event.message) ?: return
        if (command.privateReply) {
            event.user?.send()?.message(command.reply)
        } else {
            event.channel.send().message(command.reply)
        }
    }
}

class IrcLinkListener : ListenerAdapter() {
    override fun onMessage(event: MessageEvent) {
        val regex = "[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)".toRegex()
        if (event.message.contains(regex)) {
            val providedUrl = regex.find(event.message)?.value
            val url = if (!providedUrl?.contains("https?://".toRegex())!!) {
                "http://$providedUrl"
            } else {
                providedUrl
            }
            thread {
                try {
                    val connection = Jsoup.connect(url).followRedirects(true).execute()
                    val title = if (connection.url().host == "www.youtube.com") {
                        connection.parse().getElementsByTag("meta").first {
                            it.attr("property") == "og:title"
                        }.attr("content")
                    } else {
                        connection.parse().title()
                    }
                    event.channel.send().message(connection.url().host + " | " + title)
                } catch (e: UnknownHostException) {
                    // thank you javae . net !
                }
            }
        }
    }
}

fun startIrcListeners(ircConfig: IrcBotConfig, executor: CommandExecutor, enableLinkPreview: Boolean) {
    val ircConfiguration = Configuration.Builder()
        .setName(ircConfig.name)
        .addServer(ircConfig.server)
        .addAutoJoinChannel(ircConfig.channel)
        .setNickservPassword(ircConfig.password)
        .addListener(IrcCommandListener(ircConfig, executor))
        .setAutoReconnect(true)

    if (enableLinkPreview) ircConfiguration.addListener(IrcLinkListener())

    val ircBot = PircBotX(ircConfiguration.buildConfiguration())

    thread {
        ircBot.startBot()
    }
}
