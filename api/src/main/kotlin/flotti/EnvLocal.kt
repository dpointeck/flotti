package flotti

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists

internal fun loadEnv(path: Path = Path.of(".env")) = loadEnvFile(path)

private fun loadEnvFile(path: Path) {
    if (!path.exists()) return

    Files.readAllLines(path)
        .mapNotNull(::parseEnvLine)
        .forEach { (key, value) ->
            if (System.getenv(key) == null && System.getProperty(key) == null) {
                System.setProperty(key, value)
            }
        }
}

private fun parseEnvLine(line: String): Pair<String, String>? {
    val trimmed = line.trim()
    if (trimmed.isEmpty() || trimmed.startsWith('#')) return null

    val withoutExport = trimmed.removePrefix("export ").trimStart()
    val separatorIndex = withoutExport.indexOf('=')
    if (separatorIndex == -1) return null

    val key = withoutExport.substring(0, separatorIndex).trim()
    if (key.isEmpty()) return null

    val rawValue = withoutExport.substring(separatorIndex + 1).trim()
    return key to rawValue.unquote()
}

private fun String.unquote(): String = when {
    length >= 2 && first() == '"' && last() == '"' -> substring(1, lastIndex)
        .replace("\\n", "\n")
        .replace("\\r", "\r")
        .replace("\\t", "\t")
        .replace("\\\"", "\"")
        .replace("\\\\", "\\")

    length >= 2 && first() == '\'' && last() == '\'' -> substring(1, lastIndex)
    else -> substringBefore(" #").trimEnd()
}
