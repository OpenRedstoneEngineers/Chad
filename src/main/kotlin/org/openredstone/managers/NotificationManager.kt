package org.openredstone.managers

import java.awt.Color
import java.util.Timer
import kotlin.concurrent.schedule

import org.javacord.api.DiscordApi
import org.javacord.api.entity.channel.ServerTextChannel
import org.javacord.api.entity.message.embed.EmbedBuilder
import org.javacord.api.event.message.MessageCreateEvent
import org.javacord.api.event.message.reaction.ReactionAddEvent
import org.javacord.api.util.event.ListenerManager

import org.openredstone.entity.NotificationRoleConfig
import org.openredstone.toNullable

class NotificationManager(
    discordApi: DiscordApi,
    notificationChannelId: Long,
    private val notificationRoles: List<NotificationRoleConfig>
) {
    private val listeners: List<ListenerManager<out Any>>
    private val notificationMessageId: Long
    private val notificationChannel = discordApi.getServerTextChannelById(notificationChannelId)
        .orElseThrow { RuntimeException("Notification channel does not exist!") }

    init {
        notificationChannel.apply {
            deleteRecentMessagesByOthers()
            notificationMessageId = setupNotificationMessage(notificationRoles)
            listeners = listOf(
                addReactionAddListener(this@NotificationManager::reactionAdded),
                addMessageCreateListener(this@NotificationManager::messageCreated)
            )
        }
    }

    private fun reactionAdded(event: ReactionAddEvent) {
        val user = event.user
        if (user.isBot || event.messageId != notificationMessageId) {
            return
        }
        event.removeReaction().get()

        val emoji = event.emoji.asUnicodeEmoji().toNullable() ?: return
        val role = notificationRoles.firstOrNull { it.emoji == emoji }?.role ?: return
        val server = event.server.get()
        val discordRole = server.getRolesByName(role).first()
        val reply = if (user.getRoles(server).any { it.name == role }) {
            user.removeRole(discordRole)
            "<@${user.id}>, you are no longer subscribed to $role notifications."
        } else {
            user.addRole(discordRole)
            "<@${user.id}>, you are now subscribed to $role notifications."
        }

        val sent = event.channel.sendMessage(reply).get()
        Timer().schedule(5000) {
            sent.delete().get()
        }
    }

    private fun messageCreated(event: MessageCreateEvent) {
        if (!event.messageAuthor.isYourself) {
            event.deleteMessage("").get()
        }
    }
}

private val ServerTextChannel.recentMessages
    get() = getMessages(10).get()

private fun ServerTextChannel.deleteRecentMessagesByOthers() = recentMessages.forEach {
    if (!it.userAuthor.get().isYourself)
        it.delete()
}

private fun ServerTextChannel.setupNotificationMessage(notificationRoles: List<NotificationRoleConfig>): Long {
    val embedMessage = buildEmbedMessage(notificationRoles)
    val message = recentMessages.firstOrNull {
        it.userAuthor.get().isYourself
    }?.also {
        it.edit(embedMessage).get()
        it.removeAllReactions().get()
    } ?: sendMessage(embedMessage).get()

    notificationRoles.forEach {
        message.addReaction(it.emoji)
    }
    return message.id
}

private fun buildEmbedMessage(notificationRoles: List<NotificationRoleConfig>): EmbedBuilder {
    val embedBuilder = EmbedBuilder()
        .setColor(Color.decode("#cd2d0a"))
        .setTitle("Notifications")
        .setDescription("React with the corresponding emoji to toggle your notification status for each type.")
        .setFooter("⚠️note: you will automatically be banned from this channel if you abuse the bot")
        .setThumbnail("https://org.openredstone.org/wp-content/uploads/2018/07/icon-mini.png")

    notificationRoles.forEach {
        embedBuilder.addInlineField(it.name, it.description)
    }
    return embedBuilder
}
