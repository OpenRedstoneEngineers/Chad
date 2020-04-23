package org.openredstone.commands.irc

import org.javacord.api.DiscordApi
import java.util.*

class ListCommand(private var statusChannelId: Long, private var discordApi: DiscordApi)
    : IrcCommand("list", 0, true) {
    override fun runCommand(args: List<String>) {
        val stringBuilder = StringBuilder()
        val channel = discordApi.getServerTextChannelById(statusChannelId).toNullable() ?: return
        channel.getMessages(1).get().first()
            .embeds[0].fields.asSequence()
            .filter { it.isInline }
            .forEach {
                val name = it.name.replace("*", "")
                val value = it.value.replace("`", "")
                stringBuilder.append(name)
                if (!name.contains("offline") && !name.contains("(0)")) {
                    stringBuilder.append(": ").append(value)
                }
                stringBuilder.append("\n")
            }
        reply = stringBuilder.toString()
    }


    private fun <T> Optional<T>.toNullable(): T? = when {
        isPresent -> get()
        else -> null
    }

}
