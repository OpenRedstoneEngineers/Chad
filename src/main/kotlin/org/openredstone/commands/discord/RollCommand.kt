package org.openredstone.commands.discord

class RollCommand : DiscordCommand("roll") {
    private val dice = arrayOf("⚀", "⚁", "⚂", "⚃", "⚄", "⚅")

    override fun runCommand(args: List<String>) {
        reply = dice.random()
    }
}
