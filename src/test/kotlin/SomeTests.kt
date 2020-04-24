import org.junit.Test
import org.openredstone.commands.Command
import org.openredstone.commands.CommandContext
import org.openredstone.commands.CommandContext.DISCORD
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

fun fakeCommands(vararg commands: Command) =
    CommandManager(
        fakeConfig,
        commands.toList()
    )

val commandManager = fakeCommands(ApplyCommand)

fun execute(ctx: CommandContext, cmd: String, fn: AttemptedCommand.() -> Unit) =
    commandManager.getAttemptedCommand(ctx, cmd)!!.fn()

// This might seem somewhat pointless, but it also exercises the CommandManager
class `apply command` {
    @Test
    fun `requires an argument`() = execute(DISCORD, ",apply") {
        assert(reply.contains("number of arguments"))
    }

    @Test
    fun student() = execute(DISCORD, ",apply student") {
        assert(reply.contains("apply for student"))
    }

    @Test
    fun builder() = execute(DISCORD, ",apply builder") {
        assert(reply.contains("apply for builder"))
    }
}
