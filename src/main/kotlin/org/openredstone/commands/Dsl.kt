package org.openredstone.commands

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * The command function can be used to build a command.
 */
fun command(configure: CommandScope.() -> Unit) = CommandScope().apply(configure).buildCommand()

@DslMarker
annotation class CommandMarker

@CommandMarker
class ReplyScope(val sender: Sender) {
    operator fun Subcommand.invoke(vararg args: String) = command.runCommand(sender, args.toList())

    /**
     * Formats a link, depending on the service.
     */
    fun link(link: String) = when (sender.service) {
        Service.DISCORD -> "<$link>"
        Service.IRC -> link
    }
}

@CommandMarker
class CommandScope {
    /**
     * The help message. It is used to generate the help message.
     */
    var help: String? = null

    private var requiredParameters = 0
    private var optionalParameters = 0
    private var vararg = false
    private val parameters = mutableListOf<Argument>()
    // TODO: should we make replies optional?
    private var command: Command? = null

    /**
     * The reply of the command.
     */
    fun reply(isPrivate: Boolean = false, message: ReplyScope.() -> String) {
        // requireParameters = 0, so that we can return custom error messages
        command = object : Command(requireParameters = 0, privateReply = isPrivate) {
            val params = parameters.joinToString(" ")

            override fun help(name: String): String {
                val usage = "Usage: ,$name $params"
                return help?.let { "$it $usage" } ?: usage
            }

            override fun runCommand(sender: Sender, args: List<String>): String {
                if (args.size < requiredParameters) {
                    return "expected at least $requiredParameters argument(s), got ${args.size}"
                }
                val maxParameters = requiredParameters + optionalParameters
                if (!vararg && args.size > maxParameters) {
                    return "expected at most $maxParameters argument(s), got ${args.size}"
                }
                for ((i, parameter) in parameters.withIndex()) {
                    when (parameter) {
                        is Argument.Required -> parameter.value = args[i]
                        is Argument.Optional -> parameter.value = args.getOrNull(i)
                        is Argument.Default -> parameter.value = args.getOrNull(i) ?: parameter.default
                        is Argument.Vararg -> parameter.values = args.subList(i, args.size)
                    }
                }
                return ReplyScope(sender).message()
            }
        }
    }

    /**
     * A required argument. This is supposed to be used as a delegate.
     */
    fun required() = Argument.Required().also {
        if (vararg) {
            throw IllegalStateException("a required parameter after a vararg parameter is not allowed")
        }
        val last = parameters.lastOrNull()
        if (last is Argument.Optional) {
            throw IllegalStateException("a required parameter after an optional parameter is not allowed")
        }
        if (last is Argument.Default) {
            throw IllegalStateException("a required parameter after a default parameter is not allowed")
        }
        requiredParameters += 1
        parameters.add(it)
    }

    /**
     * An optional argument. This is supposed to be used as a delegate.
     */
    fun optional() = Argument.Optional().also {
        if (vararg) {
            throw IllegalStateException("an optional parameter after a vararg parameter is not allowed")
        }
        optionalParameters += 1
        parameters.add(it)
    }

    /**
     * A default argument. This is supposed to be used as a delegate.
     */
    fun default(value: String) = Argument.Default(value).also {
        if (vararg) {
            throw IllegalStateException("a default parameter after a vararg parameter is not allowed")
        }
        optionalParameters += 1
        parameters.add(it)
    }

    /**
     * Variable arguments. This is supposed to be used as a delegate.
     */
    fun vararg() = Argument.Vararg().also {
        if (vararg) {
            throw IllegalStateException("a vararg parameter after a vararg parameter is not allowed")
        }
        vararg = true
        parameters.add(it)
    }

    internal fun buildCommand() = command ?: throw IllegalStateException("no reply supplied")
}

class Subcommand(internal val command: Command)

// Arguments

sealed class Argument {
    internal abstract val name: String

    class Required : Argument() {
        internal lateinit var value: String
        override lateinit var name: String

        operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): ReadOnlyProperty<Any?, String> {
            name = property.name
            return object : ReadOnlyProperty<Any?, String> {
                override fun getValue(thisRef: Any?, property: KProperty<*>) = value
            }
        }

        override fun toString() = name
    }

    class Optional : Argument() {
        internal var value: String? = null
        override lateinit var name: String

        operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): ReadOnlyProperty<Any?, String?> {
            name = property.name
            return object : ReadOnlyProperty<Any?, String?> {
                override operator fun getValue(thisRef: Any?, property: KProperty<*>) = value
            }
        }

        override fun toString() = "[$name]"
    }

    class Default(internal val default: String) : Argument() {
        internal lateinit var value: String
        override lateinit var name: String

        operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): ReadOnlyProperty<Any?, String> {
            name = property.name
            return object : ReadOnlyProperty<Any?, String> {
                override operator fun getValue(thisRef: Any?, property: KProperty<*>) = value
            }
        }

        override fun toString() = "[$name=$default]"
    }

    class Vararg : Argument() {
        internal lateinit var values: List<String>
        override lateinit var name: String

        operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): ReadOnlyProperty<Any?, List<String>> {
            name = property.name
            return object : ReadOnlyProperty<Any?, List<String>> {
                override operator fun getValue(thisRef: Any?, property: KProperty<*>) = values
            }
        }

        override fun toString() = "[$name...]"
    }
}
