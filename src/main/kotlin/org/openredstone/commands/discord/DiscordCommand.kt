package org.openredstone.commands.discord

import org.openredstone.commands.Command
import org.openredstone.commands.CommandContext

abstract class DiscordCommand(command: String, requireParameters: Int = 0) : Command(CommandContext.DISCORD, command, null, requireParameters)
