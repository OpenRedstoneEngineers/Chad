import kotlin.test.Test

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml

import org.openredstone.CommandExecutor
import org.openredstone.CommandResponse
import org.openredstone.commands.*
import org.openredstone.entity.ChadSpec
import kotlin.test.assertEquals

fun CommandExecutor.testIrc(cmd: String, fn: CommandResponse.() -> Unit) =
    tryExecute(Sender(Service.IRC, "tester", emptyList()), cmd)!!.fn()

class `config file` {
    @Test
    fun load() {
        val config = Config { addSpec(ChadSpec) }.from.yaml.file("config.example.yaml")
        @Suppress("UNUSED_VARIABLE")
        val chadConfig = config[ChadSpec.chad]
    }
}

class `apply command` {
    private val executor = CommandExecutor(',', mapOf(
        "apply" to applyCommand
    ))

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
            val arg by required("required")
            reply { arg }
        },
        "optional" to command {
            val arg by optional("optional")
            reply { arg ?: "42" }
        },
        "vararg" to command {
            @Suppress("UNUSED_VARIABLE")
            val first by required("first")
            val rest by vararg("rest")
            reply { rest.joinToString() }
        },
        "subcommand" to command {
            val sub = Subcommand(command {
                reply { "git commit sudoku" }
            })
            reply { sub() }
        }
    ))

    @Test
    fun required() = executor.testIrc(",required lol") {
        assertEquals("lol", reply)
    }

    @Test
    fun optional() {
        executor.testIrc(",optional lol") {
            assertEquals("lol", reply)
        }
        executor.testIrc(",optional") {
            assertEquals("42", reply)
        }
    }

    @Test
    fun vararg() {
        executor.testIrc(",vararg lol 1 2 3") {
            assertEquals("1, 2, 3", reply)
        }
        executor.testIrc(",vararg lol") {
            assertEquals("", reply)
        }
    }

    @Test
    fun subcommand() {
        executor.testIrc(",subcommand") {
            assertEquals("git commit sudoku", reply)
        }
    }
}
