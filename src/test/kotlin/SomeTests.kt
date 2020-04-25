import org.junit.Test
import org.openredstone.commands.Command
import org.openredstone.commands.CommandContext
import org.openredstone.commands.CommandContext.*
import org.openredstone.commands.StaticCommand
import org.openredstone.commands.both.ApplyCommand
import org.openredstone.managers.AttemptedCommand
import org.openredstone.managers.CommandManager
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
    emptyList()
)

fun fakeCommands(vararg commands: Pair<String, Command>) =
    CommandManager(
        fakeConfig,
        commands.toMap()
    )

fun CommandManager.execute(ctx: CommandContext, cmd: String, fn: AttemptedCommand.() -> Unit): Unit = when (ctx) {
    BOTH -> {
        execute(DISCORD, cmd, fn)
        execute(IRC, cmd, fn)
    }
    else -> this.getAttemptedCommand(ctx, cmd)!!.fn()
}

// This might seem somewhat pointless, but it also exercises the CommandManager
class `apply command` {
    private val commands = fakeCommands(
        "apply" to ApplyCommand
    )

    @Test
    fun `requires an argument`() = commands.execute(BOTH, ",apply") {
        assert(reply.contains("number of arguments"))
    }

    @Test
    fun student() = commands.execute(BOTH, ",apply student") {
        assert(reply.contains("apply for student"))
    }

    @Test
    fun builder() = commands.execute(BOTH, ",apply builder") {
        assert(reply.contains("apply for builder"))
    }
}

class `can have different commands with the same name for irc and discord` {
    private val commands = fakeCommands(
        "test" to StaticCommand(DISCORD, "success discord"),
        "test" to StaticCommand(IRC, "success irc")
    )

    @Test
    fun `for discord`() = commands.execute(DISCORD, ",test") {
        assert(reply.contains("success discord"))
    }

    @Test
    fun `for irc`() = commands.execute(IRC, ",test") {
        assert(reply.contains("success irc"))
    }
}
