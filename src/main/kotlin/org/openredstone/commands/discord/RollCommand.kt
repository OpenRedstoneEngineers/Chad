package org.openredstone.commands.discord

class RollCommand : DiscordCommand("roll") {
    private val dice = arrayOf("⚀", "⚁", "⚂", "⚃", "⚄", "⚅")

    override fun runCommand(args: Array<String>) {
        reply = dice.random()
    }
}
