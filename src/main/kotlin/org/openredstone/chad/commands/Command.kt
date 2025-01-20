package org.openredstone.chad.commands

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import org.javacord.api.DiscordApi
import org.javacord.api.entity.channel.AutoArchiveDuration
import org.javacord.api.entity.channel.ChannelType
import org.javacord.api.entity.message.Message
import org.javacord.api.entity.message.MessageBuilder
import org.javacord.api.entity.message.MessageType
import org.javacord.api.entity.message.embed.EmbedBuilder
import org.javacord.api.entity.server.BoostLevel
import org.javacord.api.entity.server.Server
import org.openredstone.chad.*
import org.openredstone.chad.channelUrl
import org.openredstone.chad.commands.dsl.ReplyScope
import org.openredstone.chad.commands.dsl.command
import org.openredstone.chad.messageUrl
import org.openredstone.chad.toNullable
import java.awt.Color
import java.lang.NumberFormatException
import java.net.URLEncoder
import kotlin.NoSuchElementException
import kotlin.random.Random


/**
 * Information about the sender of a command.
 */
data class Sender(val username: String, val roles: List<String>)

typealias Commands = Map<String, Command>

/**
 * The response of a command.
 */
data class CommandResponse(val privateReply: Boolean, val reply: String, val reactions: List<String> = emptyList())

class CommandExecutor(private val commandChar: Char, private val commands: Commands) {
    companion object {
        val nameRegex = Regex("""^\S+""")
        val argRegex = Regex("""^\s+(?:"([^"]*)"|(\S+))""")

        val invalidCommand = CommandResponse(false, "Invalid command", emptyList())
        val invalidArg = CommandResponse(false, "Invalid argument", emptyList())
    }

    suspend fun tryExecute(
        sender: Sender,
        discordMessage: Message,
        message: String,
        coroutineScope: CoroutineScope,
        sql: Sql
    ): CommandResponse? {
        if (message.isEmpty() || message[0] != commandChar) {
            return null
        }

        logger.info("${sender.username} $message")
        // parse message
        var index = 1 // skip commandChar
        val nameResult = nameRegex.find(message.substring(1)) ?: return invalidCommand
        val name = nameResult.value
        val args = mutableListOf<String>()
        index += nameResult.value.length
        while (index < message.length) {
            val argResult = argRegex.find(message.substring(index)) ?: return invalidArg
            args.add(argResult.groups.let {
                it[1]?.value ?: it[2]?.value ?: return invalidArg
            })
            index += argResult.value.length
        }
        val command = commands[name] ?: return invalidCommand
        val scope = ReplyScope(sender, discordMessage, coroutineScope)

        val commandResponse = try {
            command.runCommand(scope, args).takeIf { it.reply.isNotEmpty() }
        } catch (e: Exception) {
            logger.error(e) { "caught exception while running command" }
            CommandResponse(command.privateReply, "An error occurred while running the command.")
        }
        // Insert command history
        val discordAuthor = discordMessage.userAuthor.get()
        var senderToInsert = sender.username
        val service = if (discordAuthor.isBot) {
            discordAuthor.name
        } else {
            senderToInsert = discordAuthor.idAsString
            if (discordMessage.isPrivateMessage) {
                "directmessage"
            } else {
                "discord <#${discordMessage.channel.id}>"
            }
        }
        if (commandResponse != null) {
            sql.insertHistory(
                name,
                args,
                commandResponse.reply,
                service,
                senderToInsert
            )
        }
        return commandResponse
    }
}

abstract class Command(
    val privateReply: Boolean = false,
    val notAuthorized: String = "You are not authorized to run this command.",
    private val authorizedRoles: List<String>? = null,
) {
    abstract suspend fun runCommand(replyScope: ReplyScope, args: List<String>): CommandResponse

    open fun help(name: String): String = "No help available for this command."

    fun isAuthorized(senderRoles: List<String>): Boolean {
        return authorizedRoles == null || senderRoles.intersect(authorizedRoles).isNotEmpty()
    }
}

// predefined commands

private fun baseConvert(oldBase: Int, newBase: Int, num: String): String =
    try {
        num.toLong(radix = oldBase).toString(radix = newBase)
    } catch (e: NumberFormatException) {
        e.message?.let { "Invalid number: $it" }
            ?: "Invalid number"
    }

fun shortConvertCommand(old: Int, new: Int) = command {
    check(old in 2..36 && new in 2..36)
    val num by required()
    reply {
        baseConvert(old, new, num)
    }
}

val convertCommand = command {
    val oldBase by required()
    val newBase by required()
    val num by required()
    reply {
        val old = oldBase.toIntOrNull()
        val new = newBase.toIntOrNull()
        when {
            old !in 2..36 || new !in 2..36 -> "Invalid base(s); base 2 to base 36 are supported"
            // old, new never null, because range check
            else -> baseConvert(old!!, new!!, num)
        }
    }
}

val applyCommand = command {
    help = "Instructions to apply."
    val arg by required()
    reply {
        when (arg) {
            "student" -> "To apply for student, hop onto `mc.openredstone.org` and run `/apply`"
            "builder" -> "To apply for builder, follow the steps outlined here: ${link("https://discourse.openredstone.org/builder")}"
            "engineer" -> "To apply for engineer, follow the steps outlined here: ${link("https://discourse.openredstone.org/engineer")}"
            else -> "Specify \"student\", \"builder\", or \"engineer\"."
        }
    }
}

val uuidCommand = command {
    help = "Please provide a username to query"
    val arg by required()
    reply {
        val response = khttp.get("https://api.mojang.com/users/profiles/minecraft/${arg}")
        if (response.statusCode != 200) {
            "Invalid username provided"
        } else {
            val raw = response.jsonObject.get("id").toString()
            val uuid = listOf(  // Standard UUIDs have dashes, Mojang returns it without the dashes
                raw.substring(0, 8),
                raw.substring(8, 12),
                raw.substring(12, 16),
                raw.substring(16, 20),
                raw.substring(20, 32)
            ).joinToString("-")
            "`${uuid}`"
        }
    }
}

fun helpCommand(commands: Commands) = command {
    val command by optional()
    // this has to be lazy, so that `commands` can be modified after the help command was created
    val messages by lazy { commands.mapValues { (name, cmd) -> cmd.help(name) } }
    val available by lazy { commands.keys.joinToString() }
    reply(isPrivate = true) {
        command?.let {
            messages[it] ?: "No such command available"
        } ?: "Available commands: $available"
    }
}

fun insultCommand(insults: List<String>) = command {
    reply {
        insults.random().replace("%USER%", sender.username)
    }
}

val lmgtfy = command {
    val search by vararg()
    reply {
        if (search.isEmpty()) {
            "No query provided!"
        } else {
            buildString {
                append("<https://letmegoogleforyou.com/?q=")
                append(search.joinToString("%20") {
                    URLEncoder.encode(it, "utf-8")
                })
                append(">")
            }
        }
    }
}

val rngCommand = command {
    fun generateRandomBits(length: Int) = (1..length).joinToString("") { if (Random.nextBoolean()) "1" else "0" }
    val bits by required()
    reply {
        try {
            val parsedLength = bits.toInt()
            if (parsedLength > 64) {
                "That's probably too much"
            } else {
                generateRandomBits(parsedLength)
            }
        } catch (e: NumberFormatException) {
            "Invalid bit length! I'm expecting an integer."
        }
    }
}

val rollCommand = command {
    val d6 = arrayOf("⚀", "⚁", "⚂", "⚃", "⚄", "⚅")

    help = "NdT where N is the number and T is the type of die. Sample: ,roll 2d6+10d12."
    val dice by optional()
    reply {
        when (dice) {
            null -> d6.random()
            "rick" -> link("https://youtu.be/dQw4w9WgXcQ")
            else -> {
                val split = dice!!.split("+") // non-null assertion is necessary, because dice has a custom getter
                split.map { die ->
                    val (repeat, type) = parseDie(die) ?: return@reply "Invalid dice format."
                    val values = List(repeat) { Random.nextInt(1, type + 1) }
                    val result = values.joinToString()
                    "**d$type** rolled **$repeat** time(s): `$result` (**${values.sum()}**)"
                }.joinToString(separator = "\n", prefix = "\n")
            }
        }
    }
}

val pollCommand = command {
    val question by required()
    val options by vararg()
    reply {
        if (options.isEmpty()) {
            return@reply "Polls must have at least 1 option"
        }
        if (options.size > 9) {
            return@reply "Polls can't have more than 9 options."
        }
        options
            .mapIndexed { index, option ->
                val emoji = numberEmoji(index + 1)
                reactions.add(emoji)
                "$emoji $option"
            }.joinToString(prefix = "Poll: $question\n", separator = "\n")
    }
}

/**
 * A command that always returns the same reply.
 */
fun staticCommand(message: String) = command {
    reply { message }
}

private val dieRegex = Regex("""(\d*)d(\d+)""")

private fun parseDie(die: String): Pair<Int, Int>? {
    val (repeat, type) = dieRegex.matchEntire(die)?.destructured ?: return null
    return Pair(
        if (repeat == "") 1 else repeat.toIntOrNull()?.coerceIn(1..20) ?: return null,
        type.toIntOrNull()?.coerceIn(2..128) ?: return null
    )
}

/**
 * Converts a number in the range 1..9 to a corresponding unicode emoji.
 */
private fun numberEmoji(n: Int) = when (n) {
    1 -> "\u0031\uFE0F\u20E3"
    2 -> "\u0032\uFE0F\u20E3"
    3 -> "\u0033\uFE0F\u20E3"
    4 -> "\u0034\uFE0F\u20E3"
    5 -> "\u0035\uFE0F\u20E3"
    6 -> "\u0036\uFE0F\u20E3"
    7 -> "\u0037\uFE0F\u20E3"
    8 -> "\u0038\uFE0F\u20E3"
    9 -> "\u0039\uFE0F\u20E3"
    else -> error("invalid number")
}

fun issueCommand(authorizedRoles: List<String>, discordApi: DiscordApi, chadConfig: ChadConfig, discordServer: Server) = command(authorizedRoles) {
    val topic by vararg()
    reply {
        val realTopic = topic.joinToString(" ")
        val target = message.referencedMessage.toNullable()
        if (message.type != MessageType.REPLY || target == null) {
            return@reply "Unable to create issue, I do not know what to base the issue off of"
        }
        val staffHelpChannel = discordApi.getServerTextChannelById(chadConfig.staffHelpChannelId)
            .orElseThrow { NoSuchElementException("could not get staff-help channel") }
        val duration = when (discordServer.boostLevel) {
            BoostLevel.TIER_1 -> AutoArchiveDuration.THREE_DAYS
            BoostLevel.TIER_2 -> AutoArchiveDuration.ONE_WEEK
            else -> AutoArchiveDuration.ONE_DAY
        }
        if (message.channel.id == chadConfig.staffHelpChannelId) {
            val helpChannel = staffHelpChannel.createThreadForMessage(target, realTopic, duration).await()
            arrayOf(message, target)
                .mapNotNull { it.userAuthor.toNullable() }
                .forEach { helpChannel.addThreadMember(it).await() }
            message.delete().await()
        } else {
            val helpChannel = staffHelpChannel.createThread(ChannelType.SERVER_PUBLIC_THREAD, realTopic, duration, false).await()
            message.userAuthor.toNullable()?.let { helpChannel.addThreadMember(it).await() }
            val reply = "Help topic created: ${channelUrl(chadConfig.serverId, helpChannel.id)}"
            if (!target.author.isBotUser) {
                target.userAuthor.toNullable()?.let { helpChannel.addThreadMember(it).await() }
                target.reply(reply).await()
            } else {
                message.channel.sendMessage(reply).await()
            }
            helpChannel.sendMessage(
                "Originally referenced message: ${
                    messageUrl(
                        chadConfig.serverId,
                        target.channel.id,
                        target.id
                    )
                }"
            ).await()
            message.delete().await()
        }
        ""
    }
}

fun deleteCommand(authorizedRoles: List<String>, discordApi: DiscordApi, chadConfig: ChadConfig) = command(authorizedRoles) {
    val reason by vararg()
    reply {
        val realReason = reason.joinToString(" ")
        val target = message.referencedMessage.toNullable()
        if (message.type != MessageType.REPLY || target == null) {
            return@reply "Unable to delete, I do not know what to delete"
        }
        val removedContentChannel =
            discordApi.getTextChannelById(chadConfig.removedContentChannelId)
                .orElseThrow { NoSuchElementException("could not get removed content channel") }
        val server = discordApi.getServerById(chadConfig.serverId)
            .orElseThrow { NoSuchElementException("could not get server") }
        val reply = target.reply("Message deleted by <@!${message.author.id}>: \"$realReason\"").await()
        val displayName = target.userAuthor.toNullable()?.getNickname(server)?.toNullable()
        val embed = EmbedBuilder().apply {
            setAuthor(
                "ORE Moderation Services",
                "https://youtu.be/dQw4w9WgXcQ",
                "https://openredstone.org/wp-content/uploads/2018/07/icon-mini.png"
            )
            addField("Staff Member", "<@!${message.author.id}>")
            if (displayName != null) {
                addInlineField("User", "<@!${target.author.id}>")
                addInlineField("Display Name", displayName)
            } else {
                addField("User", "<@!${target.author.id}>")
            }
            addField("Reason", realReason)
            addInlineField("Channel", "<#${reply.channel.id}>")
            addInlineField(
                "Context",
                messageUrl(chadConfig.serverId, reply.channel.id, reply.id)
            )
            setColor(Color.RED)
            setFooter("FootORE")
            setThumbnail("https://cdn.discordapp.com/emojis/892499052942463027.webp")
        }
        MessageBuilder().copy(target).addEmbed(embed).send(removedContentChannel).await()
        target.delete().await()
        message.delete().await()
        "" // totally a bad hack for now
    }
}

fun piklCommand(authorizedRoles: List<String>, discordServer: Server, discordApi: DiscordApi) = command(authorizedRoles) {
    val name by required()
    fun parseId() = Regex("""<@!?([0-9]{10,20})>""").find(name)?.groupValues?.last()
    fun gimmiePikl() = discordServer.getRolesByName("pikl")?.firstOrNull()
    reply {
        val piklRole = gimmiePikl() ?: return@reply "No pikl rank :("
        val discordId = parseId() ?: return@reply "Invalid user."
        val user = discordApi.getUserById(discordId).await()
        val roles = user.getRoles(discordServer)
        if (roles.none { role -> role.name == "pikl" }) {
            user.addRole(piklRole).await()
        }
        launch {
            delay(120_000)
            user.removeRole(piklRole).await()
        }
        "<@${discordId}> got pikl'd."
    }
}

fun trustCommand(authorizedRoles: List<String>, discordServer: Server, discordApi: DiscordApi) = command(authorizedRoles) {
    val name by required()
    fun parseId() = Regex("""<@!?([0-9]{10,20})>""").find(name)?.groupValues?.last()
    fun getTrustedRole() = discordServer.getRolesByName("Trusted")?.firstOrNull()
    reply {
        val trustedRole = getTrustedRole() ?: return@reply "No Trusted role :("
        val discordId = parseId() ?: return@reply "Invalid user."
        val user = discordApi.getUserById(discordId).await()
        val roles = user.getRoles(discordServer)
        if (roles.none { role -> role.name == "Trusted" }) {
            user.addRole(trustedRole).await()
            "<@${discordId}> is now Trusted"
        } else {
            user.removeRole(trustedRole).await()
            "<@${discordId}> is no longer Trusted"
        }
    }
}

fun historyCommand(authorizedRoles: List<String>, sql: Sql) = command(authorizedRoles) {
    data class HistoryContainer(val key: String, var count: Int, var lastRan: Int)

    val time by optional()
    reply(isPrivate = true) {
        val historyMap = mutableMapOf<String, HistoryContainer>()
        sql.getHistory().forEach {
            if (it.key in historyMap) {
                historyMap[it.key]!!.apply {
                    this.count += 1
                    this.lastRan = it.time
                }
                // The sort order is time ASC,
                // so newer runs will be later
                // in the search result
            } else {
                historyMap[it.key] = HistoryContainer(it.key, 1, it.time)
            }
        }
        historyMap.values
            .sortedWith(compareByDescending { it.count })
            .take(10).joinToString(
                prefix = "Query results less than $time seconds:\n", separator = "\n"
            ) {
                "${it.count}: ${it.key}, last ran <t:${it.lastRan}:F>"
            }
    }
}

fun fractalCommand(authorizedRoles: List<String>, chadConfig: ChadConfig) = command(authorizedRoles) {
    val seed by required()
    reply {
        val img = fractal(seed, chadConfig.fractalDeets)
        val embed = EmbedBuilder().apply {
            setImage(img, "png")
        }
        MessageBuilder().addEmbed(embed).send(message.channel).await()
        ""
    }
}
