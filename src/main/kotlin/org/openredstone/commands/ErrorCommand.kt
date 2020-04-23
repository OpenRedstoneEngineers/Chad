package org.openredstone.commands

import org.openredstone.model.context.CommandContext

class ErrorCommand : Command(CommandContext.BOTH, null, "Invalid command.") {

    override fun runCommand(args: Array<String>) {

    }

}