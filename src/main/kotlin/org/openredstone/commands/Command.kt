package org.openredstone.commands

enum class CommandContext {
    IRC, DISCORD, BOTH
}

abstract class Command(val type: CommandContext, val command: String? = null, var reply: String? = null, val requireParameters: Int = 0, val isPrivateMessageResponse: Boolean = false) {
    abstract fun runCommand(args: Array<String>)
}

class ErrorCommand : Command(CommandContext.BOTH, null, "Invalid command.") {
    override fun runCommand(args: Array<String>) {}
}

class StaticCommand(context: String, command: String, reply: String) : Command(CommandContext.valueOf(context), command, reply) {
    override fun runCommand(args: Array<String>) {}
}
