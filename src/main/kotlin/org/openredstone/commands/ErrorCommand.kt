package org.openredstone.commands

import org.openredstone.model.context.CommandContext

class ErrorCommand : Command(CommandContext.BOTH) {

    override fun runCommand(args: Array<String>) {
        // this is dum i kknow. i need halp
        reply = error
    }

}