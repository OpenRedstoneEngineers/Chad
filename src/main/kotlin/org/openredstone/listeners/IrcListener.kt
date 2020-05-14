package org.openredstone.listeners

import kotlin.concurrent.thread

import mu.KotlinLogging
import org.jsoup.Jsoup
import org.pircbotx.Configuration
import org.pircbotx.PircBotX
import org.pircbotx.hooks.ListenerAdapter
import org.pircbotx.hooks.events.MessageEvent

import org.openredstone.CommandExecutor
import org.openredstone.commands.Sender
import org.openredstone.commands.Service
import org.openredstone.entity.IrcBotConfig

private class IrcCommandListener(private val ircConfig: IrcBotConfig, private val executor: CommandExecutor) : ListenerAdapter() {
    override fun onMessage(event: MessageEvent) {
        val role = if (event.user?.channelsOpIn!!.any { ircConfig.channel == it.name }) "op" else ""
        val sender = Sender(Service.IRC, event.user?.nick.toString(), listOf(role))
        val response = executor.tryExecute(sender, event.message) ?: return
        if (response.privateReply) {
            response.reply.split("\n").forEach{event.user?.send()?.message(it)}
        } else {
            response.reply.split("\n").forEach{event.channel.send().message(it)}
        }
    }
}

private class IrcLinkListener : ListenerAdapter() {
    private val logger = KotlinLogging.logger("IRC link listener")
    private val linkRegex =
        "(https?://)?([-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b[-a-zA-Z0-9()@:%_+.~#?&/=]*)".toRegex()

    override fun onMessage(event: MessageEvent) {
        val url = extractLink(event.message) ?: return
        thread {
            try {
                logger.debug("${event.user}: ${event.message}$")

                val connection = Jsoup.connect(url).followRedirects(true).execute()
                val title = if (connection.url().host == "www.youtube.com") {
                    connection.parse().getElementsByTag("meta").first {
                        it.attr("property") == "og:title"
                    }.attr("content")
                } else {
                    connection.parse().title()
                }
                event.channel.send().message("${connection.url().host} | $title")
            } catch (e: Exception) {
                // thank you javae . net ! and tank u jay soop !
                logger.warn(e) { "caught exception" }
            }
        }
    }

    private fun extractLink(message: String): String? {
        val (matchedProto, url) = linkRegex.find(message)?.destructured ?: return null
        val proto = if (matchedProto.isEmpty()) "http://" else matchedProto
        return "$proto$url"
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
