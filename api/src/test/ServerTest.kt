package flotti

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
}
