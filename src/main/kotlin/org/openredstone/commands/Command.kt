package org.openredstone.commands

import kotlin.IllegalStateException
import kotlin.random.Random
import kotlin.reflect.KProperty

import org.javacord.api.DiscordApi

import org.openredstone.toNullable

typealias Commands = Map<String, Command>

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

object ErrorCommand : Command() {
    override fun runCommand(sender: Sender, args: List<String>) = "Invalid command."
}

class StaticCommand(private val reply: String) : Command() {
    override fun runCommand(sender: Sender, args: List<String>) = reply
}

// TODO
object HelpCommand : Command() {
    private const val helpMessage = "no u"

    override fun runCommand(sender: Sender, args: List<String>) = if (args.isEmpty()) {
        helpMessage
    } else {
        when (args[0]) {
            else -> helpMessage
        }
    }
}

class ListCommand(private val statusChannelId: Long, private val discordApi: DiscordApi) :
    Command(requireParameters = 0, privateReply = true)
{
    override fun runCommand(sender: Sender, args: List<String>) = buildString {
        val channel = discordApi.getServerTextChannelById(statusChannelId).toNullable() ?: return ""
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

object RollCommand : Command() {
    private val d6 = arrayOf("⚀", "⚁", "⚂", "⚃", "⚄", "⚅")
    override fun runCommand(sender: Sender, args: List<String>) = if (args.isEmpty()) {
        d6.random()
    } else {
        when (args[0]) {
            "d4" -> Random.nextInt(1, 4).toString()
            "d8" -> Random.nextInt(1, 8).toString()
            "d10" -> Random.nextInt(1, 10).toString()
            "d12" -> Random.nextInt(1, 12).toString()
            "d20" -> Random.nextInt(1, 20).toString()
            "rick" -> sender.service.formatLink("https://youtu.be/dQw4w9WgXcQ")
            else -> d6.random()
        }
    }
}

class InsultCommand(private val insults: List<String>) : Command(requireParameters = 1) {
    override fun runCommand(sender: Sender, args: List<String>): String {
        val targetName = if (args[0] == "me") sender.username else args[0]
        return insults.random().replace("%USER%", targetName)
    }
}

class AddCommand(roles: List<String>) : Command(authorizedRoles = roles) {
    override fun runCommand(sender: Sender, args: List<String>) = if (!isAuthorized(sender)) {
        notAuthorized
    } else {
        "authorized !" // TODO()
    }
}

class AuthorizedCommand(roles: List<String>) : Command(authorizedRoles = roles) {
    override fun runCommand(sender: Sender, args: List<String>) = if (!isAuthorized(sender)) {
        notAuthorized
    } else {
        "authorized !"
    }
}

object ApplyCommand : Command() {
    private const val errorMessage = "Specify \"builder\" or \"student\"."

    override fun runCommand(sender: Sender, args: List<String>) = if (args.isEmpty()) {
        errorMessage
    } else {
        when (args[0]) {
            "student" -> "To apply for student, hop onto `mc.openredstone.org` on 1.15.2 and run `/apply`"
            "builder" -> "To apply for builder, follow the steps outlined here: ${sender.service.formatLink("https://openredstone.org/guides/apply-build-server/")}."
            else -> errorMessage
        }
    }
}

fun Service.formatLink(link: String) = when (this) {
    Service.DISCORD -> "<$link>"
    Service.IRC -> link
}

// Command DSL

val dslCommands = mapOf(
    "test" to command {
        val arg by required("this argument is required")
        reply { "Test $arg successful ${sender.username}" }
    }
)

fun command(configure: CommandScope.() -> Unit): Command = CommandScope().apply(configure).buildCommand()

@DslMarker
annotation class CommandMarker

@CommandMarker
class ReplyScope(val sender: Sender, private val args: List<String>) {
    fun link(link: String) = when (sender.service) {
        Service.DISCORD -> "<$link>"
        Service.IRC -> link
    }
}

sealed class Argument(val description: String)

class Required(description: String) : Argument(description) {
    internal lateinit var value: String

    operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
        return value
    }
}

class Optional(description: String) : Argument(description) {
    internal var value: String? = null

    operator fun getValue(thisRef: Any?, property: KProperty<*>): String? {
        return value
    }
}

@CommandMarker
class CommandScope {
    private var requiredParameters = 0
    private val parameters = mutableListOf<Argument>()
    // TODO: should we make replies optional?
    private var command: Command? = null

    fun reply(isPrivate: Boolean = false, message: ReplyScope.() -> String) {
        // requireParameters = 0, so that we can return the custom help message
        command = object : Command(requireParameters = 0, privateReply = isPrivate) {
            private val helpMessage = buildString {
                // TODO: improve help message
                append("This command requires $requiredParameters arguments: ")
                parameters.joinToString(", ") { it.description }
            }

            override fun runCommand(sender: Sender, args: List<String>): String {
                if (args.size != parameters.size) {
                    return helpMessage
                }
                for ((a, b) in parameters zip args) {
                    when (a) {
                        is Required -> a.value = b
                        is Optional -> a.value = b
                    }
                }
                return ReplyScope(sender, args).message()
            }
        }
    }

    fun required(message: String): Required = Required(message).also {
        val last = parameters.lastOrNull()
        if (last is Optional) {
            throw IllegalStateException("a required parameter after an optional parameter is not allowed")
        }
        requiredParameters += 1
        parameters.add(it)
    }

    fun optional(message: String): Optional = Optional(message).also { parameters.add(it) }

    fun buildCommand() = command ?: throw IllegalStateException("no reply supplied")
}
