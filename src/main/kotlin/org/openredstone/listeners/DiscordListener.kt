package org.openredstone.listeners

import mu.KotlinLogging
import org.javacord.api.DiscordApi
import org.javacord.api.entity.message.Message
import org.javacord.api.entity.permission.Role
import org.openredstone.commands.CommandExecutor
import org.openredstone.commands.CommandResponse
import org.openredstone.commands.Sender
import org.openredstone.commands.Service
import org.openredstone.toNullable

val spoilerLogger = KotlinLogging.logger("Spoiler listener")

fun startDiscordListeners(discordApi: DiscordApi, executor: CommandExecutor, disableSpoilers: Boolean) {
    startDiscordCommandListener(discordApi, executor)
    if (disableSpoilers) {
        startSpoilerListener(discordApi)
    }
}

private fun startDiscordCommandListener(discordApi: DiscordApi, executor: CommandExecutor) {
    discordApi.addMessageCreateListener(fun (event) {
        val user = event.messageAuthor.asUser().toNullable() ?: return
        if (user.isBot) {
            return
        }
        val response: CommandResponse
        val messageFuture = if (event.server.isPresent) {
            val server = event.server.get()
            val roles = user.getRoles(server).map(Role::getName)
            val username = user.getDisplayName(server)
            val sender = Sender(Service.DISCORD, username, roles)
            response = executor.tryExecute(sender, event.messageContent) ?: return
            if (response.privateReply) {
                user.sendMessage(response.reply)
            } else {
                event.channel.sendMessage("$username: ${response.reply}")
            }
        } else {
            val sender = Sender(Service.DISCORD, event.messageAuthor.name, emptyList())
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
