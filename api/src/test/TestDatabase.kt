package flotti

import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

private const val DEFAULT_DATABASE_URL = "jdbc:postgresql://localhost:5432/flotti?user=postgres"
private const val MIGRATIONS_PATH = "database/migrations"

internal fun withTestDatabase(test: (databaseUrl: String) -> Unit) {
    loadEnv()

    val postgres = TestPostgres.fromEnv()
    val databaseName = "flotti_test_${System.currentTimeMillis()}"

    postgres.createDatabase(databaseName)
    try {
        postgres.runMigrations(databaseName)
        test(postgres.jdbcUrl(databaseName))
    } finally {
        postgres.dropDatabase(databaseName)
    }
}

internal fun assertDummyMigrationWasApplied(databaseUrl: String) {
    transaction(connectDatabase(databaseUrl)) {
        check(exec("SELECT COUNT(*) FROM migration_smoke_test") { it.next() } == true) {
            "Dummy migration table was not queryable"
        }
    }
}

private data class TestPostgres(
    val host: String,
    val port: Int,
    val user: String?,
    val password: String?,
) {
    private val maintenanceDatabase = connectDatabase(jdbcUrl("postgres"))

    fun jdbcUrl(databaseName: String): String = buildString {
        append("jdbc:postgresql://$host:$port/$databaseName")
        val params = listOfNotNull(
            user?.let { "user=${it.urlEncoded()}" },
            password?.let { "password=${it.urlEncoded()}" },
        )
        if (params.isNotEmpty()) append(params.joinToString("&", prefix = "?"))
    }

    fun createDatabase(databaseName: String) {
        maintenanceDatabase.executeOutsideTransaction("CREATE DATABASE $databaseName")
    }

    fun dropDatabase(databaseName: String) {
        transaction(maintenanceDatabase) {
            exec(
                """
                SELECT pg_terminate_backend(pid)
                FROM pg_stat_activity
                WHERE datname = '$databaseName'
                  AND pid <> pg_backend_pid()
                """.trimIndent(),
            )
        }
        maintenanceDatabase.executeOutsideTransaction("DROP DATABASE IF EXISTS $databaseName")
    }

    fun runMigrations(databaseName: String) {
        val process = ProcessBuilder(
            "migrate",
            "-path",
            MIGRATIONS_PATH,
            "-database",
            migrationUrl(databaseName),
            "up",
        )
            .apply { password?.let { environment()["PGPASSWORD"] = it } }
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().readText()
        check(process.waitFor(30, TimeUnit.SECONDS)) { "Timed out running migrations for $databaseName" }
        check(process.exitValue() == 0) { "Failed to run migrations for $databaseName:\n$output" }
    }

    private fun migrationUrl(databaseName: String): String = buildString {
        append("postgres://")
        user?.let {
            append(it.urlEncoded())
            password?.let { password -> append(":${password.urlEncoded()}") }
            append('@')
        }
        append("$host:$port/$databaseName?sslmode=disable")
    }

    companion object {
        fun fromEnv(): TestPostgres {
            val uri = URI(databaseUrl().removePrefix("jdbc:"))
            val query = uri.queryParameters()

            return TestPostgres(
                host = uri.host ?: "localhost",
                port = uri.port.takeUnless { it == -1 } ?: 5432,
                user = query["user"],
                password = query["password"],
            )
        }

        private fun databaseUrl(): String = System.getProperty("DATABASE_URL")
            ?: System.getenv("DATABASE_URL")
            ?: DEFAULT_DATABASE_URL
    }
}

private fun Database.executeOutsideTransaction(sql: String) {
    val connection = connector()
    val previousAutoCommit = connection.autoCommit
    try {
        connection.autoCommit = true
        val statement = connection.prepareStatement(sql, false)
        try {
            statement.executeUpdate()
        } finally {
            statement.closeIfPossible()
        }
    } finally {
        connection.autoCommit = previousAutoCommit
        connection.close()
    }
}

private fun URI.queryParameters(): Map<String, String> = rawQuery
    ?.split('&')
    ?.filter { it.isNotBlank() }
    ?.associate {
        val parts = it.split('=', limit = 2)
        parts[0].urlDecoded() to parts.getOrElse(1) { "" }.urlDecoded()
    }
    .orEmpty()

private fun String.urlEncoded(): String = URLEncoder.encode(this, StandardCharsets.UTF_8)

private fun String.urlDecoded(): String = URLDecoder.decode(this, StandardCharsets.UTF_8)
