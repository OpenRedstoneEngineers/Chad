package org.openredstone

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.Optional

internal fun <T> Optional<T>.toNullable(): T? = this.orElse(null)

internal fun <K, V> concurrentMapOf(vararg pairs: Pair<K, V>): ConcurrentMap<K, V> =
    ConcurrentHashMap<K, V>(pairs.size).apply { putAll(pairs) }

internal fun eprintln(x: Any?) = System.err.println(x)
