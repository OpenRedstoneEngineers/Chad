import org.openredstone.chad.commands.CommandExecutor
import org.openredstone.chad.commands.dsl.Subcommand
import org.openredstone.chad.commands.dsl.command
import kotlin.test.Test

class Dsl {
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
