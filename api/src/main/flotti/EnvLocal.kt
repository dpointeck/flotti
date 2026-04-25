package flotti

import io.github.cdimascio.dotenv.dotenv

internal fun loadEnv() {
    val env = dotenv {
        ignoreIfMissing = true
    }
    for (entry in env.entries()) {
        val key = entry.key
        if (System.getenv(key) == null && System.getProperty(key) == null) {
            System.setProperty(key, entry.value)
        }
    }
}
