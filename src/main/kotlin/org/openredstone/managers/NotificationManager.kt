package org.openredstone.managers

import java.awt.Color
import java.util.Timer
import kotlin.concurrent.schedule
import kotlin.RuntimeException

import org.javacord.api.DiscordApi
import org.javacord.api.entity.message.embed.EmbedBuilder
import org.javacord.api.event.message.reaction.ReactionAddEvent

import org.openredstone.entity.NotificationRoleConfig
import org.openredstone.toNullable

fun monitorNotifications(
    discordApi: DiscordApi,
    notificationChannelId: Long,
    notificationRoles: List<NotificationRoleConfig>
): Unit {
    val notificationChannel = discordApi.getServerTextChannelById(notificationChannelId)
        .orElseThrow { RuntimeException("Notification channel does not exist!") }

    notificationChannel.getMessages(10).get().filter {
        !it.userAuthor.get().isYourself
    }.forEach {
        it.delete().get()
    }

    val embedMessage = buildEmbedMessage(notificationRoles)

    val message = notificationChannel.getMessages(10).get().firstOrNull {
        it.userAuthor.get().isYourself
    }

    val notificationMessage = when {
        message != null -> {
            message.edit(embedMessage).get()
            message.removeAllReactions().get()
            message
        }
        else -> notificationChannel.sendMessage(embedMessage).get()
    }

    notificationRoles.forEach {
        notificationMessage.addReaction(it.emoji)
    }

    notificationChannel.addReactionAddListener { event ->
        reactionAdded(event, notificationMessage.id, notificationRoles, discordApi)
    }

    notificationChannel.addMessageCreateListener { event ->
        if (!event.message.author.isYourself) {
            event.message.delete()
        }
    }
}

private fun reactionAdded(
    event: ReactionAddEvent,
    notificationMessageId: Long,
    notificationRoles: List<NotificationRoleConfig>,
    discordApi: DiscordApi
) {
    val user = event.user
    val message = event.message.get()
    if (user.isBot || message.id != notificationMessageId) {
        return
    }

    event.removeReaction().get()

    val emoji = event.emoji.asUnicodeEmoji().toNullable() ?: return
    val role = notificationRoles.firstOrNull { it.emoji == emoji }?.role ?: return
    val server = event.server.get()
    val discordRole = discordApi.getRolesByName(role).first { it.server.id == server.id }
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

