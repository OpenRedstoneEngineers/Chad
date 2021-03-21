package org.openredstone.entity

import org.jetbrains.exposed.sql.Table

object Sql {
    object Command : Table("command") {
        val key = varchar("cmd_key", 128)
        val response = varchar("cmd_response", 512)
    }
}
