import kotlin.test.Test

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml

import org.openredstone.CommandExecutor
import org.openredstone.CommandResponse
import org.openredstone.commands.*
import org.openredstone.entity.ChadSpec

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
        "apply" to ApplyCommand
    ))

    @Test
    fun `no arguments`() = executor.testIRC(",apply") {
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
