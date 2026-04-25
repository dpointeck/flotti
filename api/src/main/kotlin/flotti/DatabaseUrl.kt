package flotti

import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal fun databaseUrlForJdbc(databaseUrl: String): String {
    if (databaseUrl.startsWith("jdbc:postgresql://")) return databaseUrl

    val uri = URI(databaseUrl)
    val scheme = uri.scheme
    require(scheme == "postgresql" || scheme == "postgres") {
        "DATABASE_URL must use postgresql://, postgres://, or jdbc:postgresql://"
    }

    val queryParams = uri.rawQuery
        ?.split('&')
        ?.filter { it.isNotBlank() }
        ?.map {
            val parts = it.split('=', limit = 2)
            parts[0].urlDecoded() to parts.getOrElse(1) { "" }.urlDecoded()
        }
        .orEmpty()
        .toMutableList()

    uri.rawUserInfo?.let { userInfo ->
        val parts = userInfo.split(':', limit = 2)
        queryParams.add("user" to parts[0].urlDecoded())
        parts.getOrNull(1)?.let { queryParams.add("password" to it.urlDecoded()) }
    }

    return buildString {
        append("jdbc:postgresql://")
        append(uri.host ?: "localhost")
        if (uri.port != -1) {
            append(':')
            append(uri.port)
        }
        append(uri.rawPath?.takeIf { it.isNotBlank() } ?: "/")
        if (queryParams.isNotEmpty()) {
            append('?')
            append(queryParams.joinToString("&") { (key, value) -> "${key.urlEncoded()}=${value.urlEncoded()}" })
        }
    }
}

private fun String.urlEncoded(): String = URLEncoder.encode(this, StandardCharsets.UTF_8)

private fun String.urlDecoded(): String = URLDecoder.decode(this, StandardCharsets.UTF_8)
