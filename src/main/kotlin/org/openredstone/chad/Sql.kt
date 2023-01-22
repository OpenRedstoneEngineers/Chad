package org.openredstone.chad

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object SqlCommand : Table("command") {
    val key = varchar("cmd_key", 128)
    val response = varchar("cmd_response", 512)
}

class Sql(file: String, driver: String = "org.sqlite.JDBC") {
    private val database = Database.connect("jdbc:sqlite:$file", driver)

    fun initTables() = transaction(database) {
        SchemaUtils.create(SqlCommand)
    }

    fun insertCommand(key: String, response: String) = transaction(database) {
        if (key != "pikl") { // todo check for other predefined commands
            SqlCommand.deleteWhere { SqlCommand.key eq key }
            SqlCommand.insert {
                it[SqlCommand.key] = key
                it[SqlCommand.response] = response
            }
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
