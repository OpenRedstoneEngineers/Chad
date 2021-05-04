@file:Suppress("ClassName")

import kotlin.test.Test

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml

import org.openredstone.commands.CommandExecutor
import org.openredstone.commands.CommandResponse
import org.openredstone.commands.*
import org.openredstone.entity.ChadSpec

val sender = Sender(Service.IRC, "tester", emptyList())

fun CommandExecutor.testIrc(cmd: String, fn: CommandResponse.() -> Unit) =
    tryExecute(sender, cmd)!!.fn()

class `config file` {
    @Test
    fun load() {
        val config = Config { addSpec(ChadSpec) }.from.yaml.file("config.example.yaml")

        @Suppress("UNUSED_VARIABLE")
        val chadConfig = config[ChadSpec.chad]
    }
}

class `apply command` {
    private val executor = CommandExecutor(
        ',', mapOf(
            "apply" to applyCommand
        )
    )

    @Test
    fun fish() = executor.testIrc(",apply fish") {
        assert("Specify" in reply)
    }

    @Test
    fun student() = executor.testIrc(",apply student") {
        assert("apply for student" in reply)
    }

    @Test
    fun builder() = executor.testIrc(",apply builder") {
        assert("apply for builder" in reply)
    }
}

class DSL {
    private val executor = CommandExecutor(',', mapOf(
        "required" to command {
            val arg by required()
            reply { arg }
        },
        "optional" to command {
            val arg by optional()
            reply { arg ?: "42" }
        },
        "default" to command {
            val arg by default("42")
            reply { arg }
        },
        "vararg" to command {
            @Suppress("UNUSED_VARIABLE")
            val first by required()
            val rest by vararg()
            reply { rest.joinToString() }
        },
        "subcommand" to command {
            val sub = Subcommand(command {
                reply { "git commit sudoku" }
            })
            reply { sub().reply }
        }
    ))

    @Test
    fun required() = executor.testIrc(",required lol") {
        assert("lol" in reply)
    }

    @Test
    fun optional() {
        executor.testIrc(",optional lol") {
            assert("lol" in reply)
        }
        executor.testIrc(",optional") {
            assert("42" in reply)
        }
    }

    @Test
    fun default() {
        executor.testIrc(",default lol") {
            assert("lol" in reply)
        }
        executor.testIrc(",default") {
            assert("42" in reply)
        }
    }

    @Test
    fun vararg() {
        executor.testIrc(",vararg lol 1 2 3") {
            assert("1, 2, 3" in reply)
        }
        executor.testIrc(",vararg lol") {
            assert("" in reply)
        }
    }

    @Test
    fun subcommand() {
        executor.testIrc(",subcommand") {
            assert("git commit sudoku" in reply)
        }
    }
}
