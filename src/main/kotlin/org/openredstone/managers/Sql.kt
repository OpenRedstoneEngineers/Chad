package org.openredstone.managers

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.openredstone.entity.Sql

class Sql(
    file: String,
    driver: String = "org.sqlite.JDBC"
) {
    private val database = Database.connect("jdbc:sqlite:${file}", driver)

    fun initTables() = transaction(database) {
        SchemaUtils.create(
            Sql.Command
        )
    }

    fun destroy() = transaction(database) {
        SchemaUtils.drop(
            Sql.Command
        )
    }

    fun insertCommand(key: String, response: String) = transaction(database) {
        Sql.Command.insert {
            it[Sql.Command.key] = key
            it[Sql.Command.response] = response
        }
    }

    fun getCommandResponse(key: String): String? = transaction(database) {
        Sql.Command.select {
            Sql.Command.key eq key
        }.firstOrNull()?.let {
            it[Sql.Command.response]
        }
    }

    fun getCommands(): Map<String, String> = transaction(database) {
        Sql.Command.selectAll().map {
            it[Sql.Command.key] to it[Sql.Command.response]
        }.toMap()
    }
}
