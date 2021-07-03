import org.openredstone.managers.Sql
import java.nio.file.Files
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.absolutePathString
import kotlin.io.path.deleteIfExists
import kotlin.test.Test
import kotlin.test.assertEquals

class DuplicateCommandInsert {
    @Test
    fun `inserting a command overwrites previous command with the same name`(): Unit = withTempDatabase { sql ->
        sql.initTables()
        assertEquals(emptyMap(), sql.getCommands())
        sql.insertCommand("kek", "dee")
        assertEquals(mapOf("kek" to "dee"), sql.getCommands())
        sql.insertCommand("kek", "bee")
        assertEquals(mapOf("kek" to "bee"), sql.getCommands())
    }

    @OptIn(ExperimentalPathApi::class)
    private fun withTempDatabase(block: (Sql) -> Unit) {
        val temp = Files.createTempFile("chad-test", ".db")
        val path = temp.absolutePathString()
        println("Temporary database file for test: $path")
        try {
            block(Sql(path))
        } finally {
            temp.deleteIfExists()
        }
    }
}
