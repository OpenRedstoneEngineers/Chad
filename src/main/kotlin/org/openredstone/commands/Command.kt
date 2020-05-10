package org.openredstone.commands

import org.javacord.api.DiscordApi

import org.openredstone.toNullable
import kotlin.random.Random
import kotlin.reflect.KProperty

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

val dslCommands = mapOf(
    "test" to command {
        val arg by required("this argument is required")
        reply { "Test $arg successful ${sender.username}" }
    }
)

fun command(configure: DSLCommand.() -> Unit): Command = DSLCommand().apply(configure).makeCommand()

class ReplyScope(val sender: Sender, args: List<String>)

class DSLCommand {
    // TODO: figure out a better solution than lazy
    val helpMessage by lazy {
        buildString {
            append("This command requires ${args.size} arguments: ")
            args.joinToString(", ") { it.message }
        }
    }

    // TODO: should we make replies optional?
    private var reply: (ReplyScope.() -> String)? = null

    fun reply(message: ReplyScope.() -> String) {
        reply = message
    }

    private val args = mutableListOf<Argument>()

    fun required(message: String): Argument = Argument(message).also { args.add(it) }

    // TODO: figure out optional arguments
    class Argument(val message: String) {
        lateinit var value: String
        operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
            return value
        }
    }

    fun makeCommand() = object : Command(args.size) {
        override fun runCommand(sender: Sender, suppliedArgs: List<String>): String {
            if (suppliedArgs.size != args.size) {
                return helpMessage
            }
            for ((a, b) in args zip suppliedArgs) {
                a.value = b
            }
            // fix this
            reply?.let { reply ->
                return ReplyScope(sender, suppliedArgs).reply()
            }
            return "ok"
        }
    }
}
