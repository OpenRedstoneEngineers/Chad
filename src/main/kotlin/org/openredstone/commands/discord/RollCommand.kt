package org.openredstone.commands.discord

import kotlin.random.Random

class RollCommand : DiscordCommand("roll") {

    private val dice = mapOf(
        0 to "⚀",
        1 to "⚁",
        2 to "⚂",
        3 to "⚃",
        4 to "⚄",
        5 to "⚅"
    )

    override fun runCommand(args: Array<String>) {
        reply = dice[Random.nextInt(0, 5)]
    }

}