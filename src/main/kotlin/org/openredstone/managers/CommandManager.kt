package org.openredstone.managers

import org.openredstone.commands.Command
import org.openredstone.commands.CommandContext
import org.openredstone.commands.ErrorCommand
import org.openredstone.model.entity.ConfigEntity

data class AttemptedCommand(val reply: String, val privateReply: Boolean)

class CommandManager(private val config: ConfigEntity, private val commands: Map<String, Command>) {
    fun getAttemptedCommand(commandContext: CommandContext, message: String): AttemptedCommand? {
        if (message.isEmpty() || message[0] != config.commandChar) {
            return null
        }

        val args = message.split(" ")

        val name = parseCommandName(args)
        val executedCommand = commands[name]?.let {
            if (it.type.appliesTo(commandContext)) it else null
        } ?: ErrorCommand

        return if (args.size - 1 < executedCommand.requireParameters) {
            AttemptedCommand(
                "Invalid number of arguments passed to command `$name`",
                executedCommand.privateReply
            )
        } else {
            AttemptedCommand(
                executedCommand.runCommand(args.drop(1)),
                executedCommand.privateReply
            )
        }
    }


    private fun CommandContext.appliesTo(other: CommandContext) = when (this) {
        CommandContext.BOTH -> true
        else -> other == this
    }

    private fun parseCommandName(parts: List<String>) = parts[0].substring(1)
}
