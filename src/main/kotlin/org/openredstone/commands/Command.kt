package org.openredstone.commands

enum class CommandContext {
    IRC, DISCORD, BOTH
}

abstract class Command(
    val type: CommandContext,
    val name: String = "",
    val requireParameters: Int = 0,
    val privateReply: Boolean = false
) {
    abstract fun runCommand(args: List<String>): String
}

object ErrorCommand : Command(CommandContext.BOTH, "") {
    override fun runCommand(args: List<String>) = "Invalid command."
}

class StaticCommand(context: CommandContext, name: String, private val reply: String) : Command(context, name) {
    override fun runCommand(args: List<String>) = reply
}