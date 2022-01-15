package org.openredstone.chad.commands.dsl

import org.javacord.api.entity.message.Message
import org.openredstone.chad.commands.Command
import org.openredstone.chad.commands.CommandResponse
import org.openredstone.chad.commands.Sender
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * The command function can be used to build a command using a DSL.
 */
fun command(authorizedRoles: List<String>? = null, configure: CommandScope.() -> Command): Command =
    CommandScope(authorizedRoles).configure()

/**
 * An annotation class for the command DSL.
 */
@DslMarker
annotation class CommandMarker

/**
 * Used in [CommandScope.reply].
 */
@CommandMarker
class ReplyScope(val sender: Sender) {
    /**
     * The reactions to add to the message. This only has an effect on Discord.
     */
    val reactions: MutableList<String> = mutableListOf()

    /**
     * Can be used to run a [Subcommand] with the current sender.
     */
    operator fun Subcommand.invoke(vararg args: String) = command.runCommand(sender, args.toList())

    /**
     * Formats a link.
     */
    fun link(link: String): String = "<$link>"
}

/**
 * Used in [command].
 */
@CommandMarker
class CommandScope(private val authorizedRoles: List<String>?) {
    /**
     * Used to generate the help message.
     */
    var help: String? = null

    private var requiredParameters = 0
    private var optionalParameters = 0
    private var vararg = false
    private val parameters = mutableListOf<Argument>()

    /**
     * The reply of the command. This should be the last action in [command].
     */
    fun reply(isPrivate: Boolean = false, message: ReplyScope.() -> String): Command =
        object : Command(privateReply = isPrivate, authorizedRoles = authorizedRoles) {
            private val params = parameters.joinToString(" ") // used for the help message

            override fun help(name: String): String {
                val usage = "Usage: ,$name $params"
                return help?.let { "$it $usage" } ?: usage
            }

            override fun runCommand(sender: Sender, args: List<String>): CommandResponse {
                if (!isAuthorized(sender)) return response(notAuthorized)

                if (args.size < requiredParameters) {
                    return response("expected at least $requiredParameters argument(s), got ${args.size}")
                }
                val maxParameters = requiredParameters + optionalParameters
                if (!vararg && args.size > maxParameters) {
                    return response("expected at most $maxParameters argument(s), got ${args.size}")
                }
                parameters.withIndex().forEach { (i, parameter) ->
                    when (parameter) {
                        is Argument.Required -> parameter.value = args[i]
                        is Argument.Optional -> parameter.value = args.getOrNull(i)
                        is Argument.Default -> parameter.value = args.getOrNull(i) ?: parameter.default
                        is Argument.Vararg -> parameter.values = args.subList(i, args.size)
                    }
                }
                val replyScope = ReplyScope(sender)
                return response(replyScope.message(), replyScope.reactions)
            }

            /**
             * A helper to generate a [CommandResponse].
             */
            private fun response(reply: String, reactions: List<String> = emptyList()) =
                CommandResponse(privateReply, reply, reactions)
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
}

/**
 * A wrapper around a [Command], for usage as a subcommand inside another command.
 */
class Subcommand(internal val command: Command)

// Arguments

sealed class Argument {
    class Required : Argument() {
        internal lateinit var value: String
        private lateinit var name: String

        operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): ReadOnlyProperty<Any?, String> {
            name = property.name
            return ReadOnlyProperty { _, _ -> value }
        }

        override fun toString() = name
    }

    class Optional : Argument() {
        internal var value: String? = null
        private lateinit var name: String

        operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): ReadOnlyProperty<Any?, String?> {
            name = property.name
            return ReadOnlyProperty { _, _ -> value }
        }

        override fun toString() = "[$name]"
    }

    class Default(internal val default: String) : Argument() {
        internal lateinit var value: String
        private lateinit var name: String

        operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): ReadOnlyProperty<Any?, String> {
            name = property.name
            return ReadOnlyProperty { _, _ -> value }
        }

        override fun toString() = "[$name=$default]"
    }

    class Vararg : Argument() {
        internal lateinit var values: List<String>
        private lateinit var name: String

        operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): ReadOnlyProperty<Any?, List<String>> {
            name = property.name
            return ReadOnlyProperty { _, _ -> values }
        }

        override fun toString() = "[$name...]"
    }
}
