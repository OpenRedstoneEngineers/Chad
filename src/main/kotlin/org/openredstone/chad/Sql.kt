package org.openredstone.chad

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object SqlCommand : Table("command") {
    val key = varchar("cmd_key", 128)
    val response = varchar("cmd_response", 512)
}

object SqlHistory : Table("history") {
    val key = varchar("hist_key", 128)
    val args = varchar("hist_args", 512)
    val response = varchar("hist_response", 512)
    val time = integer("hist_time").index()
    val service = varchar("hist_service", 128)
    val user = varchar("hist_user", 64)
}

class Sql(file: String, driver: String = "org.sqlite.JDBC") {
    private val database = Database.connect("jdbc:sqlite:$file", driver)

    fun initTables() = transaction(database) {
        SchemaUtils.create(SqlCommand, SqlHistory)
    }

    fun insertHistory(
        key: String,
        args: String,
        response: String,
        service: String,
        user: String
    ) = transaction(database) {
        SqlHistory.insert {
            it[SqlHistory.key] = key
            it[SqlHistory.args] = args
            it[SqlHistory.response] = response
            it[SqlHistory.time] = (System.currentTimeMillis() / 1000).toInt()
            it[SqlHistory.service] = service
            it[SqlHistory.user] = user
        }
    }

    fun insertCommand(key: String, response: String) = transaction(database) {
        SqlCommand.deleteWhere { SqlCommand.key eq key }
        SqlCommand.insert {
            it[SqlCommand.key] = key
            it[SqlCommand.response] = response
        }
    }

    fun removeCommand(key: String) = transaction(database) {
        SqlCommand.deleteWhere { SqlCommand.key eq key }
    }

    fun getCommands(): Map<String, String> = transaction(database) {
        SqlCommand.selectAll().associate {
            it[SqlCommand.key] to it[SqlCommand.response]
        }
    }
}
