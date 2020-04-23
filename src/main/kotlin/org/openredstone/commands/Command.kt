package org.openredstone.commands

import org.openredstone.model.context.CommandContext

abstract class Command(val type: CommandContext, val command: String? = null, var reply: String? = null, val requireParameters: Int = 0, val isPrivateMessageResponse: Boolean = false) {

    var error: String = "Invalid command."

    abstract fun runCommand(args: Array<String>)

}