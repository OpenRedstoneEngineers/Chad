package org.openredstone.commands.both

import org.openredstone.commands.Command
import org.openredstone.commands.CommandContext

class ApplyCommand : Command(CommandContext.BOTH, "apply", "", 1) {
    override fun runCommand(args: List<String>) {
        reply = when (args[0]) {
            "student" -> "To apply for student, hop onto `mc.openredstone.org` on 1.15.2 and run `/apply`"
            "builder" -> "To apply for builder, follow the steps outlined here: https://openredstone.org/guides/apply-build-server/."
            else -> "Specify \"builder\" or \"student\"."
        }
    }
}
