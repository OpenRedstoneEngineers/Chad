package org.openredstone.commands

import org.javacord.api.DiscordApi

import org.openredstone.toNullable

typealias Commands = Map<String, Command>

abstract class Command(
    val requireParameters: Int = 0,
    val privateReply: Boolean = false
) {
    abstract fun runCommand(args: List<String>): String
}

object ErrorCommand : Command() {
    override fun runCommand(args: List<String>) = "Invalid command."
}

class StaticCommand(private val reply: String) : Command() {
    override fun runCommand(args: List<String>) = reply
}

class ListCommand(private val statusChannelId: Long, private val discordApi: DiscordApi)
    : Command(requireParameters = 0, privateReply = true) {

    override fun runCommand(args: List<String>) = buildString {
        val channel = discordApi.getServerTextChannelById(statusChannelId).toNullable() ?: return ""
        channel.getMessages(1).get().first()
            .embeds[0].fields.drop(1).asSequence()
            .filter { !it.isInline }
            .forEach {
                val name = it.name.replace("*", "")
                val value = it.value.replace("`", "")
                append(name)
                if (!name.contains("offline") && !name.contains("(0)")) {
                    append(": $value")
                }
                append("\n")
            }
    }
}

object RollCommand : Command() {
    private val dice = arrayOf("⚀", "⚁", "⚂", "⚃", "⚄", "⚅")

    override fun runCommand(args: List<String>) = dice.random()
}

object ApplyCommand : Command(requireParameters = 1) {
    override fun runCommand(args: List<String>) = when (args[0]) {
        "student" -> "To apply for student, hop onto `mc.openredstone.org` on 1.15.2 and run `/apply`"
        "builder" -> "To apply for builder, follow the steps outlined here: https://openredstone.org/guides/apply-build-server/."
        else -> "Specify \"builder\" or \"student\"."
    }
}
