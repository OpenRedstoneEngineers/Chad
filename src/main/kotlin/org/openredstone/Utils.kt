package org.openredstone

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.Optional
import kotlin.math.max
import kotlin.math.min

internal fun <T> Optional<T>.toNullable(): T? = this.orElse(null)

internal fun Int.clamp(lower: Int, upper: Int): Int = max(lower, min(upper, this))

// currently not needed
// internal fun <K, V> Map<K, V>.toConcurrentMap(): ConcurrentMap<K, V> = ConcurrentHashMap(this)

internal fun <K, V> concurrentMapOf(vararg pairs: Pair<K, V>): ConcurrentMap<K, V> =
    ConcurrentHashMap<K, V>(pairs.size).apply { putAll(pairs) }
