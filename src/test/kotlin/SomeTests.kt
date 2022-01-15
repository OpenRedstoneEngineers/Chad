@file:Suppress("ClassName")

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml
import org.openredstone.chad.commands.CommandExecutor
import org.openredstone.chad.commands.CommandResponse
import org.openredstone.chad.commands.Sender
import org.openredstone.chad.commands.applyCommand
import org.openredstone.chad.commands.dsl.command
import org.openredstone.chad.commands.lmgtfy
import org.openredstone.chad.ChadSpec
import kotlin.test.Test
import kotlin.test.assertEquals

val sender = Sender("tester", emptyList())

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

class Commands {
    private val executor = CommandExecutor(
        ',', mapOf(
            "apply" to applyCommand,
            "authorized" to command(authorizedRoles = emptyList()) {
                reply { "yes !" }
            },
            "lmgtfy" to lmgtfy,
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

    @Test
    fun lmgtfy() = executor.testIrc(",lmgtfy \"Open Redstone Engineers\"") {
        assertEquals("https://lmgtfy.com/?q=Open+Redstone+Engineers", reply)
    }

    @Test
    fun authorized() = executor.testIrc(",authorized") {
        assert("not authorized" in reply)
    }
}

class `command exception` {
    private val executor = CommandExecutor(
        '.', mapOf(
            "rip" to command {
                reply { throw IllegalStateException("dis is not ok bro") }
            },
        )
    )

    @Test
    fun rip() = executor.testIrc(".rip") {
        assert("error" in reply)
    }
}

class `command parsing` {
    // this also tests that ' ' works as command character
    private val executor = CommandExecutor(
        ' ', mapOf(
            "id" to command {
                val args by vararg()
                reply {
                    args.joinToString()
                }
            },
        )
    )

    @Test
    fun classic() = executor.testIrc(" id yes no maybe") {
        assertEquals("yes, no, maybe", reply)
    }

    @Test
    fun string() = executor.testIrc(" id \"ban capo ?\" yes yes") {
        assertEquals("ban capo ?, yes, yes", reply)
    }

    @Test
    fun rip() = executor.testIrc(" id \"\"\"") {
        // fails for unclosed quotes
        assertEquals("Invalid argument", reply)
    }
}
