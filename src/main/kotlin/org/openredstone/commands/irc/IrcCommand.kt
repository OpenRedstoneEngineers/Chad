package org.openredstone.commands.irc

import org.openredstone.commands.Command
import org.openredstone.commands.CommandContext

abstract class IrcCommand(command: String, requireParameters: Int, privateReply: Boolean)
    : Command(CommandContext.IRC, command, null, requireParameters, privateReply)
