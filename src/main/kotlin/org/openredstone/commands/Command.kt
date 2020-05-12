package org.openredstone.commands

import kotlin.random.Random

import org.javacord.api.DiscordApi

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
    help = "instructions to apply"
    val arg by required("what to apply for")
    reply {
        when (arg) {
            "student" -> "To apply for student, hop onto `mc.openredstone.org` on 1.15.2 and run `/apply`"
            "builder" -> "To apply for builder, follow the steps outlined here: ${link("https://openredstone.org/guides/apply-build-server/")}."
            else -> "Specify \"builder\" or \"student\"."
        }
    }
}

val helpCommand = command {
   val helpMessage = "no u"

    reply {
        helpMessage
    }
}

fun insultCommand(insults: List<String>) = command {
    val target by required("target")
    reply {
        val targetName = if (target == "me") sender.username else target
        insults.random().replace("%USER%", targetName)
    }
}

val invalidCommand = command {
    @Suppress("UNUSED_VARIABLE") val args by vararg("")
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

val rollCommand =  command {
    val d6 = arrayOf("⚀", "⚁", "⚂", "⚃", "⚄", "⚅")

    val dice by optional("the kind of dice")
    reply {
        // apparently the unicode symbols don't work on IRC
        fun d6() = if (sender.service == Service.DISCORD) {
            d6.random()
        } else {
            Random.nextInt(1, 6).toString()
        }

        dice?.let {
            when (it) {
                "d4" -> Random.nextInt(1, 4).toString()
                "d6" -> d6()
                "d8" -> Random.nextInt(1, 8).toString()
                "d10" -> Random.nextInt(1, 10).toString()
                "d12" -> Random.nextInt(1, 12).toString()
                "d20" -> Random.nextInt(1, 20).toString()
                "rick" -> link("https://youtu.be/dQw4w9WgXcQ")
                else -> d6.random()
            }
        } ?: d6()
    }
}

fun staticCommand(message: String) = command {
    reply { message }
}
