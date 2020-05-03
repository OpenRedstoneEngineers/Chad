import kotlin.test.Test

import org.openredstone.CommandExecutor
import org.openredstone.CommandResponse
import org.openredstone.commands.*

fun CommandExecutor.testIRC(cmd: String, fn: CommandResponse.() -> Unit) =
    tryExecute(Sender(Service.IRC, "tester", emptyList()), cmd)!!.fn()

class `apply command` {
    private val executor = CommandExecutor(',', mapOf(
        "apply" to ApplyCommand
    ))

    @Test
    fun `requires an argument`() = executor.testIRC(",apply") {
        assert(reply.contains("number of arguments"))
    }

    @Test
    fun student() = executor.testIRC(",apply student") {
        assert(reply.contains("apply for student"))
    }

    @Test
    fun builder() = executor.testIRC(",apply builder") {
        assert(reply.contains("apply for builder"))
    }
}
