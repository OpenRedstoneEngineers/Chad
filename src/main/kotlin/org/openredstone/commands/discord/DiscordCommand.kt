package org.openredstone.commands.discord

import org.openredstone.commands.Command
import org.openredstone.commands.CommandContext

abstract class DiscordCommand(requireParameters: Int = 0)
    : Command(CommandContext.DISCORD, requireParameters)

object RollCommand : DiscordCommand() {
    private val dice = arrayOf("⚀", "⚁", "⚂", "⚃", "⚄", "⚅")

    override fun runCommand(args: List<String>) = dice.random()
}
