package flotti

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.sql.DriverManager
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class ServerTest {

    @Test
    fun `root endpoint reports api and database health`() = withTestDatabase { databaseUrl ->
        assertDummyMigrationWasApplied(databaseUrl)

        testApplication {
            environment {
                config = MapApplicationConfig(
                    "postgres.url" to databaseUrl,
                    "postgres.user" to POSTGRES_USER,
                    "postgres.password" to POSTGRES_PASSWORD,
                )
            }
            application {
                configureSerialization()
                configureRouting()
            }

            val response = client.get("/")

            assertEquals(HttpStatusCode.OK, response.status)
            val body = Json.parseToJsonElement(response.body<String>()).jsonObject
            assertEquals("running", body.getValue("api").jsonPrimitive.content)
            assertEquals("connected", body.getValue("database").jsonPrimitive.content)
        }
    }

    private fun withTestDatabase(test: (databaseUrl: String) -> Unit) {
        val databaseName = "flotti-test-${System.currentTimeMillis()}"
        createDatabase(databaseName)
        try {
            runMigrations(databaseName)
            test("jdbc:postgresql://localhost:5432/$databaseName")
        } finally {
            dropDatabase(databaseName)
        }
    }

    private fun createDatabase(databaseName: String) {
        DriverManager.getConnection(POSTGRES_MAINTENANCE_URL, POSTGRES_USER, POSTGRES_PASSWORD).use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate("CREATE DATABASE ${databaseName.toPostgresIdentifier()}")
            }
        }
    }

    private fun runMigrations(databaseName: String) {
        val databaseUrl = "postgres://$POSTGRES_USER@localhost:5432/$databaseName?sslmode=disable"
        val process = ProcessBuilder(
            "migrate",
            "-path",
            MIGRATIONS_PATH,
            "-database",
            databaseUrl,
            "up",
        )
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().readText()
        val completed = process.waitFor(30, TimeUnit.SECONDS)
        check(completed) { "Timed out running migrations for $databaseName" }
        check(process.exitValue() == 0) { "Failed to run migrations for $databaseName:\n$output" }
    }

    private fun assertDummyMigrationWasApplied(databaseUrl: String) {
        DriverManager.getConnection(databaseUrl, POSTGRES_USER, POSTGRES_PASSWORD).use { connection ->
            connection.prepareStatement("SELECT COUNT(*) FROM migration_smoke_test").use { statement ->
                statement.executeQuery().use { resultSet ->
                    check(resultSet.next()) { "Dummy migration table was not queryable" }
                }
            }
        }
    }

    private fun dropDatabase(databaseName: String) {
        DriverManager.getConnection(POSTGRES_MAINTENANCE_URL, POSTGRES_USER, POSTGRES_PASSWORD).use { connection ->
            connection.createStatement().use { statement ->
                statement.execute(
                    """
                    SELECT pg_terminate_backend(pid)
                    FROM pg_stat_activity
                    WHERE datname = '$databaseName'
                      AND pid <> pg_backend_pid()
                    """.trimIndent(),
                )
                statement.executeUpdate("DROP DATABASE IF EXISTS ${databaseName.toPostgresIdentifier()}")
            }
        }
    }

    private fun String.toPostgresIdentifier(): String = '"' + replace("\"", "\"\"") + '"'

    private companion object {
        const val POSTGRES_MAINTENANCE_URL = "jdbc:postgresql://localhost:5432/postgres"
        const val POSTGRES_USER = "postgres"
        const val POSTGRES_PASSWORD = ""
        const val MIGRATIONS_PATH = "database/migrations"
    }
}
