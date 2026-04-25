package flotti

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.sql.DriverManager

@Serializable
data class HealthResponse(
    val api: String,
    val database: String,
)

fun Application.configureRouting() {
    val postgresUrl = databaseUrlForJdbc(environment.config.property("postgres.url").getString())

    routing {
        get("/") {
            val databaseConnected = checkDatabaseConnection(postgresUrl)
            if (databaseConnected) {
                call.respond(
                    HttpStatusCode.OK,
                    HealthResponse(api = "running", database = "connected"),
                )
            } else {
                call.respond(
                    HttpStatusCode.ServiceUnavailable,
                    HealthResponse(api = "running", database = "disconnected"),
                )
            }
        }
    }
}

private suspend fun checkDatabaseConnection(url: String): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        Class.forName("org.postgresql.Driver")
        DriverManager.getConnection(url).use { connection ->
            connection.prepareStatement("SELECT 1").use { statement ->
                statement.executeQuery().use { resultSet -> resultSet.next() }
            }
        }
    }.getOrDefault(false)
}
