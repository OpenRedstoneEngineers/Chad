package org.openredstone.commands

import kotlin.reflect.KProperty

/**
 * TODO
 */
fun command(configure: CommandScope.() -> Unit) = CommandScope().apply(configure).buildCommand()

@DslMarker
annotation class CommandMarker

@CommandMarker
class ReplyScope(val sender: Sender, private val args: List<String>) {
    fun link(link: String) = when (sender.service) {
        Service.DISCORD -> "<$link>"
        Service.IRC -> link
    }
}

@CommandMarker
class CommandScope {
    var help: String? = null

    private var requiredParameters = 0
    private var optionalParameters = 0
    private var vararg = false
    private val parameters = mutableListOf<Argument>()
    // TODO: should we make replies optional?
    private var command: Command? = null

    fun reply(isPrivate: Boolean = false, message: ReplyScope.() -> String) {
        // requireParameters = 0, so that we can return the custom help message
        command = object : Command(requireParameters = 0, privateReply = isPrivate) {
            // TODO: improve help message

            override fun runCommand(sender: Sender, args: List<String>): String {
                if (!vararg) {
                    if (args.size < requiredParameters) {
                        return "expected at least $requiredParameters arguments, got ${args.size}"
                    }
                    val maxParameters = requiredParameters + optionalParameters
                    if (args.size > maxParameters) {
                        return "expected at most $maxParameters arguments, got ${args.size}"
                    }
                }
                for ((i, parameter) in parameters.withIndex()) {
                    when (parameter) {
                        is Required -> parameter.value = args[i]
                        is Optional -> parameter.value = args.getOrNull(i)
                        is VarArg -> parameter.values = args.subList(i, args.size)
                    }
                }
                return ReplyScope(sender, args).message()
            }
        }
    }

    fun required(message: String): Required = Required(message).also {
        if (vararg) {
            throw IllegalStateException("a required parameter after a vararg parameter is not allowed")
        }
        val last = parameters.lastOrNull()
        if (last is Optional) {
            throw IllegalStateException("a required parameter after an optional parameter is not allowed")
        }
        requiredParameters += 1
        parameters.add(it)
    }

    fun optional(message: String): Optional = Optional(message).also {
        if (vararg) {
            throw IllegalStateException("an optional parameter after a vararg parameter is not allowed")
        }
        optionalParameters += 1
        parameters.add(it)
    }

    fun vararg(message: String): VarArg = VarArg(message).also {
        if (vararg) {
            throw IllegalStateException("a vararg parameter after a vararg parameter is not allowed")
        }
        vararg = true
        parameters.add(it)
    }

    fun buildCommand() = command ?: throw IllegalStateException("no reply supplied")
}

// Arguments

sealed class Argument(val description: String)

class Required(description: String) : Argument(description) {
    internal lateinit var value: String

    operator fun getValue(thisRef: Any?, property: KProperty<*>) = value
}

class Optional(description: String) : Argument(description) {
    internal var value: String? = null

    operator fun getValue(thisRef: Any?, property: KProperty<*>) = value
}

class VarArg(description: String) : Argument(description) {
    internal lateinit var values: List<String>

    operator fun getValue(thisRef: Any?, property: KProperty<*>) = values
}
