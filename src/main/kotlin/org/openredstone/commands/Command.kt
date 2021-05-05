package org.openredstone.commands

import org.javacord.api.DiscordApi
import org.openredstone.commands.dsl.command
import org.openredstone.logger
import org.openredstone.toNullable
import java.net.URLEncoder
import kotlin.random.Random

/**
 * The service of the sender.
 */
enum class Service { DISCORD, IRC }

/**
 * Information about the sender of a command.
 */
data class Sender(val service: Service, val username: String, val roles: List<String>)

/**
 * A list of authorized roles for Discord and IRC. `null` means that every role is authorized.
 */
data class AuthorizedRoles(val discord: List<String>? = null, val irc: List<String>? = null)

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

    fun tryExecute(sender: Sender, message: String): CommandResponse? {
        if (message.isEmpty() || message[0] != commandChar) {
            return null
        }

        logger.info("${sender.username} [${sender.service}]: $message")

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

        return try {
            command.runCommand(sender, args)
        } catch (e: Exception) {
            logger.error(e) { "caught exception while running command" }
            CommandResponse(command.privateReply, "An error occurred while running the command.")
        }
    }
}

abstract class Command(
    val privateReply: Boolean = false,
    val notAuthorized: String = "You are not authorized to run this command.",
    private val authorizedRoles: AuthorizedRoles = AuthorizedRoles(),
) {
    abstract fun runCommand(sender: Sender, args: List<String>): CommandResponse

    open fun help(name: String): String = "No help available for this command."

    fun isAuthorized(sender: Sender): Boolean {
        val roles = when (sender.service) {
            Service.DISCORD -> authorizedRoles.discord
            Service.IRC -> authorizedRoles.irc
        }
        return roles == null || sender.roles.intersect(roles).isNotEmpty()
    }
}

// predefined commands

val applyCommand = command {
    help = "Instructions to apply."
    val arg by required()
    reply {
        when (arg) {
            "student" -> "To apply for student, hop onto `mc.openredstone.org` and run `/apply`"
            "builder" -> "To apply for builder, follow the steps outlined here: ${link("https://discourse.openredstone.org/builder")}."
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

fun listCommand(statusChannelId: Long, discordApi: DiscordApi) = command {
    reply(isPrivate = true) {
        buildString {
            val channel = discordApi.getServerTextChannelById(statusChannelId).toNullable() ?: return@reply ""
            channel.getMessages(1).get().first()
                .embeds[0].fields.drop(1)
                .filter { !it.isInline }
                .forEach {
                    val name = it.name.replace("*", "")
                    val value = it.value.replace("`", "")
                    append(name)
                    if (!name.contains("offline") && !name.contains("(0)")) {
                        append(": $value")
                    }
                    append("\n")
                }
        }
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
