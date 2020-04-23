package org.openredstone.managers

import org.javacord.api.DiscordApi
import org.javacord.api.entity.message.embed.EmbedBuilder
import org.openredstone.model.entity.NotificationRoleEntity
import java.awt.Color
import java.util.*
import kotlin.concurrent.schedule

class NotificationManager(val discordApi: DiscordApi, val notificationChannel: Long, val notificationRoleEntities: List<NotificationRoleEntity>) {

    var notificationMessageId = Optional.empty<Long>()

    fun setupNotificationMessage() {
        val channelOpt = discordApi.getServerTextChannelById(notificationChannel)
        if (channelOpt.isPresent) {
            val channel = channelOpt.get()
            channel.getMessages(10).get().forEach { if (!it.userAuthor.get().isYourself) it.delete() }
            val message = channel.getMessages(10).get().stream().filter { it.userAuthor.get().isYourself } .findFirst()
            if (message.isPresent) {
                notificationMessageId = Optional.of(message.get().id)
                message.get().edit(getEmbeddedMessage())
                message.get().removeAllReactions()
            } else {
                notificationMessageId = Optional.of(channel.sendMessage(getEmbeddedMessage()).get().id)
            }
            val notificationMessage = channel.getMessageById(notificationMessageId.get()).get()
            notificationRoleEntities.forEach { notificationMessage.addReaction(it.emoji) }
        } else {
            println("Notification channel does not exist!")
        }
    }

    fun monitorNotifications() {
        discordApi.serverTextChannels.stream().filter { channel ->
            channel.id == notificationChannel
        } .findFirst().ifPresent { channel ->
            channel.addReactionAddListener { event ->
                if (!event.user.isBot && (event.message.get().id == notificationMessageId.get())) {
                    event.emoji.asUnicodeEmoji().ifPresent { emoji ->
                        if ( notificationRoleEntities.any { it.emoji == emoji } ) {
                            val role = notificationRoleEntities.first { it.emoji == emoji } .role
                            val user = event.user
                            val discordRole = discordApi.getRolesByName(role).first { it.server.id == event.server.get().id }
                            val reply =
                                    if (user.getRoles(event.server.get()).any { it.name == role }) {
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