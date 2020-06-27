package org.openredstone.commands

import org.javacord.api.DiscordApi
import org.openredstone.clamp
import org.openredstone.logger
import org.openredstone.toNullable
import java.net.URLEncoder
import kotlin.random.Random

enum class Service { DISCORD, IRC }

data class Sender(val service: Service, val username: String, val roles: List<String>)

data class AuthorizedRoles(val discord: List<String>?, val irc: List<String>?)

typealias Commands = Map<String, Command>

data class CommandResponse(val reply: String, val privateReply: Boolean)

class CommandExecutor(private val commandChar: Char, private val commands: Commands) {
    fun tryExecute(sender: Sender, message: String): CommandResponse? {
        if (message.isEmpty() || message[0] != commandChar) {
            return null
        }

        logger.info("${sender.username} [${sender.service}]: $message")

        val parts = message.split(Regex("""\s+"""))
        val args = parts.drop(1)
        val name = parts[0].substring(1)
        val command = commands[name] ?: return CommandResponse("Invalid command.", false)

        val reply = if (args.size < command.requireParameters) {
            "Not enough arguments passed to command `$name`, expected at least ${command.requireParameters}."
        } else {
            try {
                command.runCommand(sender, args)
            } catch (e: Exception) {
                logger.error(e) { "caught exception while running command" }

                "An error occurred while running the command."
            }
        }
        return CommandResponse(reply, command.privateReply)
    }
}

abstract class Command(
    val requireParameters: Int = 0,
    val privateReply: Boolean = false,
    private val authorizedRoles: AuthorizedRoles = AuthorizedRoles(null, null),
    val notAuthorized: String = "You are not authorized to run this command."
) {
    fun isAuthorized(sender: Sender) = when (sender.service) {
        Service.DISCORD -> isAuthorized(sender, authorizedRoles.discord)
        Service.IRC -> isAuthorized(sender, authorizedRoles.irc)
    }

    private fun isAuthorized(sender: Sender, roles: List<String>?) =
        roles == null || sender.roles.intersect(roles).isNotEmpty()

    open fun help(name: String) = "No help available for this command."

    abstract fun runCommand(sender: Sender, args: List<String>): String
}


val applyCommand = command {
    help = "Instructions to apply."
    val arg by required()
    reply {
        when (arg) {
            "student" -> "To apply for student, hop onto `mc.openredstone.org` on 1.15.2 and run `/apply`"
            "builder" -> "To apply for builder, follow the steps outlined here: ${link("https://openredstone.org/guides/apply-build-server/")}."
            else -> "Specify \"builder\" or \"student\"."
        }
    }
}

fun helpCommand(commands: Commands) = command {
    val command by optional()
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
    val search by vararg()
    reply {
        buildString {
            append("https://lmgtfy.com/?q=")
            append(search.joinToString("+") {
                URLEncoder.encode(it, "utf-8")
            })
        }
    }
}

val rollCommand = command {
    val d6 = arrayOf("⚀", "⚁", "⚂", "⚃", "⚄", "⚅")

    val dice by optional()
    help = "NdT where N is the number and T is the type of die. Sample: ,roll 2d6+10d12."
    reply {
        when (dice) {
            null -> d6.random()
            "rick" -> link("https://youtu.be/dQw4w9WgXcQ")
            else -> {
                val split = dice!!.split("+")  // non-null assertion is necessary, because dice has a custom getter
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

private fun parseDie(die: String): Pair<Int, Int>? {
    val (repeat, type) = Regex("""(\d*)d(\d+)""").matchEntire(die)?.destructured ?: return null
    return Pair(
        if (repeat == "") 1 else repeat.toIntOrNull()?.clamp(1, 20) ?: return null,
        type.toIntOrNull()?.clamp(2, 128) ?: return null
    )
}

fun staticCommand(message: String) = command {
    reply { message }
}
