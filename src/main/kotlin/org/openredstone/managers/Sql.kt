package org.openredstone.managers

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.openredstone.entity.Sql

class Sql(file: String, driver: String = "org.sqlite.JDBC") {
    private val database = Database.connect("jdbc:sqlite:$file", driver)

    fun initTables() = transaction(database) {
        SchemaUtils.create(Sql.Command)
    }

    fun insertCommand(key: String, response: String) = transaction(database) {
        Sql.Command.deleteWhere { Sql.Command.key eq key }
        Sql.Command.insert {
            it[Sql.Command.key] = key
            it[Sql.Command.response] = response
        }
    }

    fun removeCommand(key: String) = transaction(database) {
        Sql.Command.deleteWhere { Sql.Command.key eq key }
    }

    fun getCommands(): Map<String, String> = transaction(database) {
        Sql.Command.selectAll().associate {
            it[Sql.Command.key] to it[Sql.Command.response]
        }
    }
}
