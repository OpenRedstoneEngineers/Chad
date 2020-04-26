package org.openredstone.managers

import java.awt.Color
import java.util.*
import kotlin.concurrent.schedule
import kotlin.properties.Delegates

import org.javacord.api.DiscordApi
import org.javacord.api.entity.channel.ServerTextChannel
import org.javacord.api.entity.message.embed.EmbedBuilder

import org.openredstone.entity.NotificationRoleConfig
import org.openredstone.toNullable

class NotificationManager(private val discordApi: DiscordApi, private val notificationChannel: Long, private val notificationRoleEntities: List<NotificationRoleConfig>) {
    private var notificationMessageId: Long by Delegates.notNull()

    fun setupNotificationMessage() {
        val channelOpt: ServerTextChannel? = discordApi.getServerTextChannelById(notificationChannel).toNullable()
        channelOpt?.let { channel ->
            channel.getMessages(10).get().forEach { if (!it.userAuthor.get().isYourself) it.delete() }
            val message = channel.getMessages(10).get().asSequence().firstOrNull { it.userAuthor.get().isYourself }

            if (message != null) {
                notificationMessageId = message.id
                message.edit(getEmbeddedMessage())
                message.removeAllReactions()
            } else {
                notificationMessageId = channel.sendMessage(getEmbeddedMessage()).get().id
            }
            val notificationMessage = channel.getMessageById(notificationMessageId).get()
            notificationRoleEntities.forEach { notificationMessage.addReaction(it.emoji) }
        } ?: println("Notification channel does not exist!")
    }

    fun monitorNotifications() {
        discordApi.serverTextChannels.asSequence().firstOrNull { channel ->
            channel.id == notificationChannel
        }?.let { channel ->
            channel.addReactionAddListener { event ->
                if (!event.user.isBot && (event.message.get().id == notificationMessageId)) {
                    event.emoji.asUnicodeEmoji().ifPresent { emoji ->
                        if (notificationRoleEntities.any { it.emoji == emoji }) {
                            val role = notificationRoleEntities.first { it.emoji == emoji }.role
                            val user = event.user
                            val discordRole = discordApi.getRolesByName(role).first { it.server.id == event.server.get().id }
                            val reply = if (user.getRoles(event.server.get()).any { it.name == role }) {
                                event.user.removeRole(discordRole)
                                "<@${user.id}>, you are no longer subscribed to $role notifications."
                            } else {
                                event.user.addRole(discordRole)
                                "<@${user.id}>, you are now subscribed to $role notifications."
                            }

                            val sent = event.channel.sendMessage(reply).get()

                            Timer().schedule(5000) {
                                sent.delete().get()
                            }
                        }
                    }
                    event.removeReaction()
                }
            }

            channel.addMessageCreateListener { event ->
                if (!event.message.author.isYourself) {
                    event.message.delete()
                }
            }
        }
    }

    private fun getEmbeddedMessage(): EmbedBuilder {
        val embedBuilder = EmbedBuilder()
            .setColor(Color.decode("#cd2d0a"))
            .setTitle("Notifications")
            .setDescription("React with the corresponding emoji to toggle your notification status for each type.")
            .setFooter("⚠️note: you will automatically be banned from this channel if you abuse the bot")
            .setThumbnail("https://org.openredstone.org/wp-content/uploads/2018/07/icon-mini.png")

        notificationRoleEntities.forEach { embedBuilder.addInlineField(it.name, it.description) }

        return embedBuilder
    }
}
