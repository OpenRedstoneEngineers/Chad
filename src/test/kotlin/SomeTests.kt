import kotlin.test.Test

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml

import org.openredstone.CommandExecutor
import org.openredstone.CommandResponse
import org.openredstone.commands.*
import org.openredstone.entity.ChadSpec
import kotlin.test.assertEquals

fun CommandExecutor.testIRC(cmd: String, fn: CommandResponse.() -> Unit) =
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
    fun fish() = executor.testIRC(",apply fish") {
        assert("Specify" in reply)
    }

    @Test
    fun student() = executor.testIRC(",apply student") {
        assert("apply for student" in reply)
    }

    @Test
    fun builder() = executor.testIRC(",apply builder") {
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
            @Suppress("UNUSED_VARIABLE") val first by required("first")
            val rest by vararg("rest")
            reply { rest.joinToString() }
        }
    ))

    @Test
    fun required() = executor.testIRC(",required lol") {
        assertEquals("lol", reply)
    }

    @Test
    fun optional() {
        executor.testIRC(",optional lol") {
            assertEquals("lol", reply)
        }
        executor.testIRC(",optional") {
            assertEquals("42", reply)
        }
    }

    @Test
    fun vararg() {
        executor.testIRC(",vararg lol 1 2 3") {
            assertEquals("1, 2, 3", reply)
        }
        executor.testIRC(",vararg lol") {
            assertEquals("", reply)
        }
    }
}
