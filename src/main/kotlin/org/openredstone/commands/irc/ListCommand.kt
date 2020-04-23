package org.openredstone.commands.irc

import org.javacord.api.DiscordApi

class ListCommand(private var statusChannelId: Long, private var discordApi: DiscordApi)
    : IrcCommand("list", 0, true) {
    override fun runCommand(args: Array<String>) {
        val stringBuilder = StringBuilder()
        discordApi.getServerTextChannelById(statusChannelId).ifPresent { channel ->
            channel.getMessages(1).get().forEach { message ->
                message.embeds[0].fields.forEach {
                    if (it.isInline) {
                        stringBuilder.append(it.name.replace("*",""))
                        if (!it.name.contains("offline")
                            && !it.name.replace("*","").contains("(0)")) {
                            stringBuilder.append(": ").append(it.value.replace("`",""))
                        }
                        stringBuilder.append("\n")
                    }
                }
            }
        }
        reply = stringBuilder.toString()
    }
}
