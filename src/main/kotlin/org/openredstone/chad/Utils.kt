package org.openredstone.chad

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
