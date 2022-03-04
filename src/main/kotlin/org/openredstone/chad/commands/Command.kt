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
import java.net.URLEncoder
import java.util.*
import kotlin.NoSuchElementException
import kotlin.concurrent.schedule
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

    suspend fun tryExecute(sender: Sender, discordMessage: Message, message: String, coroutineScope: CoroutineScope): CommandResponse? {
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

        return try {
            command.runCommand(scope, args).takeIf { it.reply.isNotEmpty() }
        } catch (e: Exception) {
            logger.error(e) { "caught exception while running command" }
            CommandResponse(command.privateReply, "An error occurred while running the command.")
        }
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
    val target by required()
    reply {
        val targetName = if (target == "me") sender.username else target
        insults.random().replace("%USER%", targetName)
    }
}

val lmgtfy = command {
    val search by required()
    reply {
        buildString {
            append("https://lmgtfy.com/?q=")
            append(search.split("\\s+").joinToString("+") {
                URLEncoder.encode(it, "utf-8")
            })
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

