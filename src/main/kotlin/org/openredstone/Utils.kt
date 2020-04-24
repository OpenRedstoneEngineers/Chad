package org.openredstone

import java.util.Optional

internal fun <T> Optional<T>.toNullable(): T? = this.orElse(null)
