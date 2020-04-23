package org.openredstone.commands

enum class CommandContext {
    IRC, DISCORD, BOTH
}

abstract class Command(
    val type: CommandContext,
    val command: String = "",
    var reply: String,
    val requireParameters: Int = 0,
    val isPrivateMessageResponse: Boolean = false
) {
    abstract fun runCommand(args: List<String>)
}

class ErrorCommand : Command(CommandContext.BOTH, "", "Invalid command.") {
    override fun runCommand(args: List<String>) {}
}

class StaticCommand(context: String, command: String, reply: String) : Command(CommandContext.valueOf(context), command, reply) {
    override fun runCommand(args: List<String>) {}
}
