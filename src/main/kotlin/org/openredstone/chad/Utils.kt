package org.openredstone.chad

import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

internal fun <T> Optional<T>.toNullable(): T? = this.orElse(null)

internal fun <K, V> concurrentMapOf(vararg pairs: Pair<K, V>): ConcurrentMap<K, V> =
    ConcurrentHashMap<K, V>(pairs.size).apply { putAll(pairs) }

internal fun eprintln(x: Any?) = System.err.println(x)
