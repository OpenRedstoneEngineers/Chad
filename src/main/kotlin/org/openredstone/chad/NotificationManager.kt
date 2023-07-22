package org.openredstone.chad

import org.javacord.api.DiscordApi
import org.javacord.api.entity.channel.ServerTextChannel
import org.javacord.api.entity.message.MessageBuilder
import org.javacord.api.entity.message.MessageFlag
import org.javacord.api.entity.message.component.ActionRow
import org.javacord.api.entity.message.component.Button
import org.javacord.api.entity.message.embed.EmbedBuilder
import org.javacord.api.event.interaction.MessageComponentCreateEvent
import org.javacord.api.event.message.MessageCreateEvent
import org.javacord.api.event.message.reaction.ReactionAddEvent
import org.javacord.api.util.event.ListenerManager
import java.awt.Color

class NotificationManager(
    discordApi: DiscordApi,
    notificationChannelId: Long,
    private val notificationRoles: List<NotificationRoleConfig>,
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
                addMessageCreateListener(this@NotificationManager::messageCreated),
                addMessageComponentCreateListener(this@NotificationManager::interactionCreated)
            )
        }
    }

    private fun reactionAdded(event: ReactionAddEvent) {
        event.removeReaction().get()
    }

    private fun messageCreated(event: MessageCreateEvent) {
        if (!event.messageAuthor.isYourself and !event.message.flags.contains(MessageFlag.EPHEMERAL)) {
            event.deleteMessage("").get()
        }
    }

    private fun interactionCreated(event: MessageComponentCreateEvent) {
        event.interaction.user
        val user = event.interaction.user
        val id = event.messageComponentInteraction.customId
        val role = notificationRoles.firstOrNull { it.role.lowercase() == id }?.role ?: return
        val server = event.interaction.server.get()
        val discordRole = server.getRolesByName(role).first()
        val reply = if (user.getRoles(server).any { it.name == role }) {
            user.removeRole(discordRole)
            "<@${user.id}>, you are no longer subscribed to $role notifications."
        } else {
            user.addRole(discordRole)
            "<@${user.id}>, you are now subscribed to $role notifications."
        }
        event.interaction
            .createImmediateResponder()
            .setContent(reply)
            .setFlags(MessageFlag.EPHEMERAL)
            .respond()
    }
}

private fun ServerTextChannel.setupNotificationMessage(notificationRoles: List<NotificationRoleConfig>): Long {
    val embedMessage = buildEmbedMessage(notificationRoles)
    recentMessages.firstOrNull {
        it.userAuthor.get().isYourself
    }?.also {
        it.delete()
    }
    val messageBuilder = MessageBuilder().apply {
        this.setEmbed(embedMessage)
        this.addComponents(
            ActionRow.of(notificationRoles.map {
                Button.primary(it.role.lowercase(), it.role)
            })
        )
    }
    return messageBuilder.send(this).get().id
}

private fun buildEmbedMessage(notificationRoles: List<NotificationRoleConfig>): EmbedBuilder {
    val embedBuilder = EmbedBuilder()
        .setColor(Color.decode("#cd2d0a"))
        .setTitle("Notifications")
        .setDescription("Click the corresponding button to toggle your notification status for each type.")
        .setFooter("⚠️note: you will automatically be banned from this channel if you abuse the bot")
        .setThumbnail("https://org.openredstone.org/wp-content/uploads/2018/07/icon-mini.png")

    notificationRoles.forEach {
        embedBuilder.addInlineField(it.name, it.description)
    }
    return embedBuilder
}
