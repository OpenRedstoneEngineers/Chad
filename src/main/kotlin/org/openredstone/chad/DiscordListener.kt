package org.openredstone.chad

import khttp.post
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import mu.KLogger
import mu.KotlinLogging
import org.javacord.api.DiscordApi
import org.javacord.api.entity.message.Message
import org.javacord.api.entity.message.embed.EmbedBuilder
import org.javacord.api.entity.permission.Role
import org.javacord.api.event.message.MessageCreateEvent
import org.openredstone.chad.commands.CommandExecutor
import org.openredstone.chad.commands.CommandResponse
import org.openredstone.chad.commands.Sender

val spoilerLogger = KotlinLogging.logger("Spoiler listener")

internal fun Message.getUrl(): String {
    val server = this.server.toNullable()?.id ?: throw Exception("Oof")
    val channel = this.channel.id
    return "https://discord.com/channels/${server}/${channel}/${this.id}"
}

fun startDiscordListeners(
    logger: KLogger,
    discordApi: DiscordApi,
    executor: CommandExecutor,
    disableSpoilers: Boolean,
    botAutomod: Boolean,
    automodChannelId: Long,
    automodRegexes: List<String>,
    welcomeChannel: Long,
    greetings: List<String>,
    ingameBotRole: String,
    gameChatChannelId: Long,
    sql: Sql,
    coroutineScope: CoroutineScope
) {
    startDiscordCommandListener(discordApi, executor, ingameBotRole, gameChatChannelId, coroutineScope, sql)
    if (disableSpoilers) {
        startSpoilerListener(discordApi, coroutineScope)
    }
    if (botAutomod) {
        startBotAutomod(logger, discordApi, automodChannelId, automodRegexes)
    }
    if (greetings.isNotEmpty()) {
        startJoinListener(discordApi, welcomeChannel, greetings, coroutineScope)
    }
}

private fun startJoinListener(discordApi: DiscordApi, welcomeChannel: Long, greetings: List<String>, coroutineScope: CoroutineScope) {
    val channel = discordApi.getTextChannelById(welcomeChannel)
        .orElseThrow { NoSuchElementException("welcome channel not found") }
    discordApi.addServerMemberJoinListener {
        coroutineScope.launch {
            channel.sendMessage(greetings.random().replace("@USER", "<@${it.user.id}>")).await()
        }
    }
}

private fun startBotAutomod(
    logger: KLogger,
    discordApi: DiscordApi,
    automodChannelId: Long,
    automodRegexes: List<String>
) {
    val automodChannel =
        discordApi.getChannelById(automodChannelId).toNullable()?.asServerTextChannel()?.toNullable() ?: run {
            logger.error { "Could not locate AutoMod channel" }
            return
        }
    val compiledRegexes = automodRegexes.map { Regex(it, RegexOption.IGNORE_CASE) }
    fun onBotMessage(event: MessageCreateEvent) {
        val user = event.messageAuthor.asUser().toNullable() ?: return
        if (discordApi.clientId == user.id || !user.isBot) {
            return
        }
        val body = event.message.content
        val matches = compiledRegexes.filter { it.containsMatchIn(body) }
        if (matches.isNotEmpty()) {
            val messageUrl = event.message.getUrl()
            logger.warn("Flagged message for bot AutoMod! (Matches ${matches}) $messageUrl")
            val embed = EmbedBuilder().setDescription(body)
            matches.forEach { embed.addField("Matches", it.pattern) }
            automodChannel.sendMessage("Flagged a message $messageUrl", embed)
        }
    }
    discordApi.addMessageCreateListener { event ->
        try {
            onBotMessage(event)
        } catch (e: Exception) {
            logger.error(e) { "onBotMessage error" }
        }
    }
}

private fun startDiscordCommandListener(
    discordApi: DiscordApi,
    executor: CommandExecutor,
    ingameBotRole: String,
    gameChatChannelId: Long,
    coroutineScope: CoroutineScope,
    sql: Sql
) {
    suspend fun onDiscordCommand(event: MessageCreateEvent) {
        val server = event.server.toNullable()
        val user = event.messageAuthor.asUser().toNullable() ?: return
        if (event.channel.id == gameChatChannelId &&
            user.isBot &&
            // :ore_ogag:
            discordApi.getRoleById(ingameBotRole).toNullable() in user.getRoles(server) &&
            !user.isYourself
        ) {
            inGameListener(event, executor, coroutineScope, sql)
        }
        if (user.isBot) {
            return
        }
        val response: CommandResponse
        val message = if (server != null) {
            val roles = user.getRoles(server).map(Role::getName)
            val username = user.getDisplayName(server)
            val sender = Sender(username, roles)
            response = executor.tryExecute(sender, event.message, event.messageContent, coroutineScope, sql) ?: return
            if (response.privateReply) {
                user.sendMessage(snipped(response.reply)).await()
            } else {
                event.channel.sendMessage(snipped("$username: ${response.reply}")).await()
            }
        } else {
            val sender = Sender(event.messageAuthor.name, emptyList())
            response = executor.tryExecute(sender, event.message, event.messageContent, coroutineScope, sql) ?: return
            user.sendMessage(snipped(response.reply)).await()
        }
        for (reaction in response.reactions) {
            message.addReaction(reaction)
        }
    }
    discordApi.addMessageCreateListener { event ->
        coroutineScope.launch {
            try {
                onDiscordCommand(event)
            } catch (e: Exception) {
                logger.error(e) { "onDiscordCommand threw" }
            }
        }
    }
}

private fun snipped(response: String) =
    if (response.length < 512) {
        response
    } else {
        // TODO: coroutinize
        val paste = post(
            url = "https://dpaste.com/api/v2/",
            headers = mapOf("User-Agent" to "ORE Chad"),
            data = mapOf(
                "content" to response,
                "syntax" to "text",
                "title" to "ORE Chad"
            )
        ).text
        "${response.substring(0, 64)} ... Snipped: $paste"
    }

private val inGameRegex = Regex("""^`[A-Za-z]+` \*\*([A-Za-z0-9_\\]+)\*\*:  (.*)$""")

private suspend fun inGameListener(
    event: MessageCreateEvent,
    executor: CommandExecutor,
    coroutineScope: CoroutineScope,
    sql: Sql
) {
    val rawMessage = event.message.content
    val (sender, message) = inGameRegex.matchEntire(rawMessage)?.destructured ?: return
    val commandSender = Sender(sender.replace("\\", ""), emptyList())
    val response = executor.tryExecute(commandSender, event.message, message, coroutineScope, sql) ?: return
    event.channel.sendMessage(
        if (response.privateReply) {
            "$sender: I can't private message to in-game yet!"
        } else {
            "$sender: ${response.reply}"
        }
    ).await()
}

private val spoilerRegex = Regex("""\|\|(?s)(.+)\|\|""")

private fun startSpoilerListener(discordApi: DiscordApi, coroutineScope: CoroutineScope) {
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

    suspend fun Message.spoilerCheck() {
        if (this.containsSpoiler()) {
            spoilerLogger.debug("${this.author} [${this.channel}]: ${this.content}")
            this.delete().await()
        }
    }

    discordApi.addMessageCreateListener {
        coroutineScope.launch { it.message.spoilerCheck() }
    }
    discordApi.addMessageEditListener {
        coroutineScope.launch { it.message.spoilerCheck() }
    }
}
