package flotti

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.sql.DriverManager
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class ServerTest {

    @Test
    fun `postgres url is converted to jdbc url`() {
        assertEquals(
            "jdbc:postgresql://localhost:5434/flotti?user=postgres&password=postgres",
            databaseUrlForJdbc("postgresql://postgres:postgres@localhost:5434/flotti"),
        )
    }

    @Test
    fun `root endpoint reports api and database health`() = withTestDatabase { databaseUrl ->
        assertDummyMigrationWasApplied(databaseUrl)

        testApplication {
            environment {
                config = MapApplicationConfig(
                    "postgres.url" to databaseUrl,
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
            test(POSTGRES_CONFIG.jdbcUrl(databaseName))
        } finally {
            dropDatabase(databaseName)
        }
    }

    private fun createDatabase(databaseName: String) {
        DriverManager.getConnection(POSTGRES_CONFIG.maintenanceUrl).use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate("CREATE DATABASE ${databaseName.toPostgresIdentifier()}")
            }
        }
    }

    private fun runMigrations(databaseName: String) {
        val process = ProcessBuilder(
            "migrate",
            "-path",
            MIGRATIONS_PATH,
            "-database",
            POSTGRES_CONFIG.migrationUrl(databaseName),
            "up",
        )
            .apply { POSTGRES_CONFIG.password?.let { environment()["PGPASSWORD"] = it } }
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().readText()
        val completed = process.waitFor(30, TimeUnit.SECONDS)
        check(completed) { "Timed out running migrations for $databaseName" }
        check(process.exitValue() == 0) { "Failed to run migrations for $databaseName:\n$output" }
    }

    private fun assertDummyMigrationWasApplied(databaseUrl: String) {
        DriverManager.getConnection(databaseUrl).use { connection ->
            connection.prepareStatement("SELECT COUNT(*) FROM migration_smoke_test").use { statement ->
                statement.executeQuery().use { resultSet ->
                    check(resultSet.next()) { "Dummy migration table was not queryable" }
                }
            }
        }
    }

    private fun dropDatabase(databaseName: String) {
        DriverManager.getConnection(POSTGRES_CONFIG.maintenanceUrl).use { connection ->
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

    private data class PostgresConfig(
        val host: String,
        val port: Int,
        val user: String?,
        val password: String?,
    ) {
        val maintenanceUrl: String = jdbcUrl("postgres")

        fun jdbcUrl(databaseName: String): String = buildString {
            append("jdbc:postgresql://")
            append(host)
            append(':')
            append(port)
            append('/')
            append(databaseName)

            val params = listOfNotNull(
                user?.let { "user=${it.urlEncoded()}" },
                password?.let { "password=${it.urlEncoded()}" },
            )
            if (params.isNotEmpty()) {
                append('?')
                append(params.joinToString("&"))
            }
        }

        fun migrationUrl(databaseName: String): String = buildString {
            append("postgres://")
            user?.let {
                append(it.urlEncoded())
                password?.let { password ->
                    append(':')
                    append(password.urlEncoded())
                }
                append('@')
            }
            append(host)
            append(':')
            append(port)
            append('/')
            append(databaseName)
            append("?sslmode=disable")
        }
    }

    private companion object {
        init {
            loadEnv()
        }

        val POSTGRES_CONFIG = parsePostgresConfig(
            System.getProperty("DATABASE_URL")
                ?: System.getenv("DATABASE_URL")
                ?: "postgresql://postgres@localhost:5432/flotti",
        )
        const val MIGRATIONS_PATH = "database/migrations"

        fun parsePostgresConfig(databaseUrl: String): PostgresConfig {
            val uri = URI(databaseUrl.removePrefix("jdbc:"))
            val queryParams = uri.rawQuery
                ?.split('&')
                ?.filter { it.isNotBlank() }
                ?.associate {
                    val parts = it.split('=', limit = 2)
                    parts[0].urlDecoded() to parts.getOrElse(1) { "" }.urlDecoded()
                }
                .orEmpty()

            val userInfo = uri.rawUserInfo?.split(':', limit = 2)
            return PostgresConfig(
                host = uri.host ?: "localhost",
                port = if (uri.port == -1) 5432 else uri.port,
                user = queryParams["user"] ?: userInfo?.getOrNull(0)?.urlDecoded(),
                password = queryParams["password"] ?: userInfo?.getOrNull(1)?.urlDecoded(),
            )
        }

        fun String.urlEncoded(): String = URLEncoder.encode(this, StandardCharsets.UTF_8)

        fun String.urlDecoded(): String = URLDecoder.decode(this, StandardCharsets.UTF_8)
    }
}
