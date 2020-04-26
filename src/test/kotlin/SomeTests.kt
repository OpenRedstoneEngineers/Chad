import org.junit.Test

import org.openredstone.commands.Command
import org.openredstone.commands.Commands
import org.openredstone.commands.StaticCommand
import org.openredstone.commands.ApplyCommand
import org.openredstone.AttemptedCommand
import org.openredstone.getAttemptedCommand
import org.openredstone.model.entity.ConfigEntity
import org.openredstone.model.entity.IrcBotEntity

val fakeConfig = ConfigEntity(
    "",
    0L,
    0L,
    "",
    ',',
    false,
    IrcBotEntity("", "", "", ""),
    emptyList(),
    mapOf(),
    mapOf()
)

fun Commands.execute(cmd: String, fn: AttemptedCommand.() -> Unit) = getAttemptedCommand(fakeConfig, cmd, this)!!.fn()

// This might seem somewhat pointless, but it also exercises the CommandManager
class `apply command` {
    private val commands = mapOf(
        "apply" to ApplyCommand
    )

    @Test
    fun `requires an argument`() = commands.execute(",apply") {
        assert(reply.contains("number of arguments"))
    }

    @Test
    fun student() = commands.execute(",apply student") {
        assert(reply.contains("apply for student"))
    }

    @Test
    fun builder() = commands.execute(",apply builder") {
        assert(reply.contains("apply for builder"))
    }
}

class `can have different commands with the same name for irc and discord` {
    private val discordCommands = mapOf(
        "test" to StaticCommand("success discord")
    )
    private val ircCommands = mapOf(
        "test" to StaticCommand("success irc")
    )

    @Test
    fun `for discord`() = discordCommands.execute(",test") {
        assert(reply.contains("success discord"))
    }

    @Test
    fun `for irc`() = ircCommands.execute(",test") {
        assert(reply.contains("success irc"))
    }
}
