package flotti

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

internal fun connectDatabase(databaseUrl: String): Database = Database.connect(
    url = databaseUrl,
    driver = "org.postgresql.Driver",
)

internal suspend fun Database.isConnected(): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        transaction(this@isConnected) {
            exec("SELECT 1") { resultSet -> resultSet.next() } ?: false
        }
    }.getOrDefault(false)
}
