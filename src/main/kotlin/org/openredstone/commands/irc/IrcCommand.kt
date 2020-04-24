package org.openredstone.commands.irc

import org.javacord.api.DiscordApi

import org.openredstone.commands.Command
import org.openredstone.commands.CommandContext
import org.openredstone.toNullable

abstract class IrcCommand(command: String, requireParameters: Int, privateReply: Boolean)
    : Command(CommandContext.IRC, command, requireParameters, privateReply)

class ListCommand(private var statusChannelId: Long, private var discordApi: DiscordApi)
    : IrcCommand("list", 0, true) {
    override fun runCommand(args: List<String>): String {
        val stringBuilder = StringBuilder()
        val channel = discordApi.getServerTextChannelById(statusChannelId).toNullable() ?: return ""
        channel.getMessages(1).get().first()
            .embeds[0].fields.drop(1).asSequence()
            .filter { !it.isInline }
            .forEach {
                val name = it.name.replace("*", "")
                val value = it.value.replace("`", "")
                stringBuilder.append(name)
                if (!name.contains("offline") && !name.contains("(0)")) {
                    stringBuilder.append(": ").append(value)
                }
                stringBuilder.append("\n")
            }
        return stringBuilder.toString()
    }
}
