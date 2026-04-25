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
    val postgresUrl = environment.config.property("postgres.url").getString()
    val postgresUser = environment.config.property("postgres.user").getString()
    val postgresPassword = environment.config.propertyOrNull("postgres.password")?.getString().orEmpty()

    routing {
        get("/") {
            val databaseConnected = checkDatabaseConnection(postgresUrl, postgresUser, postgresPassword)
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

private suspend fun checkDatabaseConnection(url: String, user: String, password: String): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        Class.forName("org.postgresql.Driver")
        DriverManager.getConnection(url, user, password).use { connection ->
            connection.prepareStatement("SELECT 1").use { statement ->
                statement.executeQuery().use { resultSet -> resultSet.next() }
            }
        }
    }.getOrDefault(false)
}
