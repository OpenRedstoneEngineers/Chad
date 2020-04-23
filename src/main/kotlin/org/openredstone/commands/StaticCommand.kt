package org.openredstone.commands

import org.openredstone.model.context.CommandContext

class StaticCommand (context: String, command: String, reply: String) : Command(CommandContext.valueOf(context), command, reply) {

    override fun runCommand(args: Array<String>) { }

}