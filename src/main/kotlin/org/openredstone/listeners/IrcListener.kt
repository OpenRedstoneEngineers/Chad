package org.openredstone.listeners

import kotlin.concurrent.thread

import mu.KotlinLogging
import org.jsoup.Jsoup
import org.pircbotx.Configuration
import org.pircbotx.PircBotX
import org.pircbotx.hooks.ListenerAdapter
import org.pircbotx.hooks.events.MessageEvent

import org.openredstone.commands.CommandExecutor
import org.openredstone.commands.CommandResponse
import org.openredstone.commands.Sender
import org.openredstone.commands.Service
import org.openredstone.entity.IrcBotConfig
import org.pircbotx.hooks.events.PrivateMessageEvent

val regex = Regex("\\x03..(.*)\\x0f")

private class IrcCommandListener(private val ircConfig: IrcBotConfig, private val executor: CommandExecutor) : ListenerAdapter() {
    override fun onMessage(event: MessageEvent) {
        if (event.user?.nick == "ORENetwork") {
            ingameListener(event)
        } else if (event.user?.nick != "OREDiscord") {
            ircListener(event)
        }
    }

    private fun ingameListener(event: MessageEvent) {
        val parsed = event.message.replace(regex) { it.groupValues[1] }
        val parsedSender = parsed.substring(0, parsed.indexOf(':'))
        val parsedMessage = parsed.substring(parsed.indexOf(':') + 2)
        val commandSender = Sender(Service.IRC, parsedSender, emptyList())
        val response = executor.tryExecute(commandSender, parsedMessage) ?: return
        response.sendResponse(if (response.privateReply) {
            { reply -> event.user?.send()!!.message("$parsedSender $reply") }
        } else {
            { reply -> event.channel.send().message("$parsedSender: $reply") }
        })
    }

    private fun ircListener(event: MessageEvent) {
        val role = if (event.user?.channelsOpIn!!.any { ircConfig.channel == it.name }) "op" else ""
        val sender = event.user?.nick!!
        val commandSender = Sender(Service.IRC, sender, listOf(role))
        val response = executor.tryExecute(commandSender, event.message) ?: return
        response.sendResponse(if (response.privateReply) {
            { reply -> event.user?.send()!!.message(reply) }
        } else {
            { reply -> event.channel.send().message("$sender: $reply") }
        })
    }

    private inline fun CommandResponse.sendResponse(send: (String) -> Unit) {
        reply.split("\n").forEach(send)
    }
}

private class IrcLinkListener : ListenerAdapter() {
    private val logger = KotlinLogging.logger("IRC link listener")
    private val linkRegex =
        Regex("""(https?://)?([-a-zA-Z0-9@:%._+~#=]{1,256}\.[a-zA-Z0-9()]{1,6}\b[-a-zA-Z0-9()@:%_+.~#?&/=]*)""")

    override fun onMessage(event: MessageEvent) {
        if (event.user?.nick == "OREDiscord") {
            val parsed = event.message.replace(regex) { it.groupValues[1] }
            if (parsed.substring(0, parsed.indexOf(':')) == "Chad") return
        }
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
