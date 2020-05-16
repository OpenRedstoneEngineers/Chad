package org.openredstone

import java.util.Optional
import kotlin.math.max
import kotlin.math.min

internal fun <T> Optional<T>.toNullable(): T? = this.orElse(null)

internal fun Int.clamp(lower: Int, upper: Int): Int = max(lower, min(upper, this))
