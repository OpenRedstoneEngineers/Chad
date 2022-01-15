package org.openredstone.listeners

import mu.KotlinLogging
import org.javacord.api.DiscordApi
import org.javacord.api.entity.message.Message
import org.javacord.api.entity.permission.Role
import org.javacord.api.event.message.MessageCreateEvent
import org.openredstone.commands.CommandExecutor
import org.openredstone.commands.CommandResponse
import org.openredstone.commands.Sender
import org.openredstone.toNullable

val spoilerLogger = KotlinLogging.logger("Spoiler listener")

fun startDiscordListeners(
    discordApi: DiscordApi,
    executor: CommandExecutor,
    disableSpoilers: Boolean,
    welcomeChannel: Long,
    greetings: List<String>,
    ingameBotRole: String,
    gameChatChannelId: Long,
) {
    startDiscordCommandListener(discordApi, executor, ingameBotRole, gameChatChannelId)
    if (disableSpoilers) {
        startSpoilerListener(discordApi)
    }
    if (greetings.isNotEmpty()) {
        startJoinListener(discordApi, welcomeChannel, greetings)
    }
}

private fun startJoinListener(discordApi: DiscordApi, welcomeChannel: Long, greetings: List<String>) {
    val channel = discordApi.getTextChannelById(welcomeChannel).get()
    discordApi.addServerMemberJoinListener {
        channel.sendMessage(greetings.random().replace("@USER", "<@${it.user.id}>"))
    }
}

private fun startDiscordCommandListener(
    discordApi: DiscordApi,
    executor: CommandExecutor,
    ingameBotRole: String,
    gameChatChannelId: Long,
) {
    discordApi.addMessageCreateListener(fun(event) {
        val server = event.server.toNullable()
        val user = event.messageAuthor.asUser().toNullable() ?: return
        if (event.channel.id == gameChatChannelId &&
            user.isBot &&
            // :ore_ogag:
            discordApi.getRoleById(ingameBotRole).toNullable() in user.getRoles(server) &&
            !user.isYourself
        ) {
            inGameListener(event, executor)
        }
        if (user.isBot) {
            return
        }
        val response: CommandResponse
        val messageFuture = if (server != null) {
            val roles = user.getRoles(server).map(Role::getName)
            val username = user.getDisplayName(server)
            val sender = Sender(username, roles)
            response = executor.tryExecute(sender, event.messageContent) ?: return
            if (response.privateReply) {
                user.sendMessage(response.reply)
            } else {
                event.channel.sendMessage("$username: ${response.reply}")
            }
        } else {
            val sender = Sender(event.messageAuthor.name, emptyList())
            response = executor.tryExecute(sender, event.messageContent) ?: return
            user.sendMessage(response.reply)
        }
        messageFuture.thenAccept {
            for (reaction in response.reactions) {
                it.addReaction(reaction)
            }
        }
    })
}

private val inGameRegex = Regex("""^`[A-Za-z]+` \*\*([A-Za-z0-9_\\]+)\*\*:  (.*)$""")

private fun inGameListener(event: MessageCreateEvent, executor: CommandExecutor) {
    val rawMessage = event.message.content
    val (sender, message) = inGameRegex.matchEntire(rawMessage)?.destructured ?: return
    val commandSender = Sender(sender.replace("\\", ""), emptyList())
    val response = executor.tryExecute(commandSender, message) ?: return
    event.channel.sendMessage(
        if (response.privateReply) {
            "$sender: I can't private message to in-game yet!"
        } else {
            "$sender: ${response.reply}"
        }
    )
}

private val spoilerRegex = Regex("""\|\|(?s)(.+)\|\|""")

private fun startSpoilerListener(discordApi: DiscordApi) {
    fun Message.containsSpoiler(): Boolean {
        var startingIndex = 0
        val content = this.content ?: return false
        Regex("""(?s)`{3}(?:(?!```).)+`{3}|`[^`]+`""").findAll(content).forEach {
            val prefix = startingIndex until it.range.first
            if (content.substring(prefix).contains(spoilerRegex)) return true
            startingIndex = it.range.last + 1
        }
        return content.substring(startingIndex).contains(spoilerRegex)
    }

    fun Message.spoilerCheck() {
        if (this.containsSpoiler()) {
            spoilerLogger.debug("${this.author} [${this.channel}]: ${this.content}")
            this.delete()
        }
    }

    discordApi.addMessageCreateListener { it.message.spoilerCheck() }
    discordApi.addMessageEditListener { it.message.ifPresent { message -> message.spoilerCheck() } }
}
