package org.openredstone.commands

import kotlin.random.Random

import org.javacord.api.DiscordApi

import org.openredstone.Commands
import org.openredstone.toNullable

enum class Service { DISCORD, IRC }

data class Sender(val service: Service, val username: String, val roles: List<String>)

abstract class Command(
    val requireParameters: Int = 0,
    val privateReply: Boolean = false,
    val authorizedRoles: List<String> = listOf("@"),
    val notAuthorized: String = "You are not authorized to run this command."
) {
    fun isAuthorized(sender: Sender): Boolean =
        "@" in authorizedRoles || sender.roles.intersect(authorizedRoles).isNotEmpty()

    open fun help(name: String) = "No help available for this command."

    abstract fun runCommand(sender: Sender, args: List<String>): String
}

class AddCommand(roles: List<String>) : Command(authorizedRoles = roles) {
    override fun runCommand(sender: Sender, args: List<String>) = if (!isAuthorized(sender)) {
        notAuthorized
    } else {
        "authorized !"  // TODO
    }
}

class AuthorizedCommand(roles: List<String>) : Command(authorizedRoles = roles) {
    override fun runCommand(sender: Sender, args: List<String>) = if (!isAuthorized(sender)) {
        notAuthorized
    } else {
        "authorized !"
    }
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
    reply {
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

val invalidCommand = command {
    @Suppress("UNUSED_VARIABLE") val args by vararg()
    reply { "Invalid command." }
}

fun listCommand(statusChannelId: Long, discordApi: DiscordApi) = command {
    reply {
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

val rollCommand = command {
    val d6 = arrayOf("⚀", "⚁", "⚂", "⚃", "⚄", "⚅")
    val dice by default("d6")
    help = "NdT where N is the number and T is the type of die.\nSample: ,roll 2d6+10d12\n"
    reply {
        if (dice == "rick") return@reply "https://youtu.be/dQw4w9WgXcQ"
        if (dice == "d6") return@reply d6.random()
        val split = dice.split("+")
        split.joinToString("\n") {
            val multiplier: Int
            val type: Int
            if (it.indexOf('d') == 0) {
                multiplier = 1
                type = it.substring(1).toInt()
            } else {
                multiplier = it.substring(0, it.indexOf('d')).toInt()
                type = it.substring(it.indexOf('d')+1).toInt()
            }
            val values = (1..multiplier).map { Random.nextInt(1, type + 1) }
            val result = values.joinToString(", ")
            "*d$type* rolled *$multiplier* time(s): `$result` (*${values.sum()}*)"
        }
    }
}

fun staticCommand(message: String) = command {
    reply { message }
}
