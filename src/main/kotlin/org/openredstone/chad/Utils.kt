package org.openredstone.chad

import org.javacord.api.entity.channel.ServerTextChannel
import org.javacord.api.entity.message.MessageSet
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

internal fun <T> Optional<T>.toNullable(): T? = this.orElse(null)

internal fun <K, V> concurrentMapOf(vararg pairs: Pair<K, V>): ConcurrentMap<K, V> =
    ConcurrentHashMap<K, V>(pairs.size).apply { putAll(pairs) }

internal fun eprintln(x: Any?) = System.err.println(x)

internal fun channelUrl(server: Long, channel: Long) =
    "https://discord.com/channels/${server}/${channel}"

internal fun messageUrl(server: Long, channel: Long, message: Long) =
    "${channelUrl(server, channel)}/${message}"

val ServerTextChannel.recentMessages: MessageSet
    get() = getMessages(10).get()

fun ServerTextChannel.deleteRecentMessagesByOthers() = recentMessages.forEach {
    if (!it.userAuthor.get().isYourself)
        it.delete()
}