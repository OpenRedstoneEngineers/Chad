package org.openredstone.listeners

import org.javacord.api.DiscordApi

object GeneralListener {
    fun startSpoilerListener(discordApi: DiscordApi) {
        discordApi.addMessageCreateListener { event ->
            if (event.message.content.contains(Regex("\\|\\|"))) {
                event.message.delete()
            }
        }
    }
}

