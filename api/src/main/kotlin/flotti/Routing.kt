package flotti

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val api: String,
    val database: String,
)

fun Application.configureRouting() {
    val database = connectDatabase(environment.config.property("postgres.url").getString())

    routing {
        get("/") {
            val databaseConnected = database.isConnected()
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
