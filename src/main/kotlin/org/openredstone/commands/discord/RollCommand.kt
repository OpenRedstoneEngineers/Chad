package org.openredstone.commands.discord

import kotlin.random.Random

class RollCommand : DiscordCommand("roll") {
    private val dice = arrayOf("⚀", "⚁", "⚂", "⚃", "⚄", "⚅")

    override fun runCommand(args: Array<String>) {
        reply = dice[Random.nextInt(0, 5)]
    }
}
