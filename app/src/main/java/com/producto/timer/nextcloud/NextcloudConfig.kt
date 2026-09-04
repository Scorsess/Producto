package com.producto.timer.nextcloud

import okhttp3.Credentials

/**
 * Stores Nextcloud connection configuration.
 */
data class NextcloudConfig(
    val serverUrl: String,
    val username: String,
    val appPassword: String
) {
    /**
     * Produces a clean HTTP Basic auth header value.
     */
    fun basicAuthHeader(): String {
        return Credentials.basic(username.trim(), appPassword.trim())
    }

    /**
     * Resolves the full URL to query user calendars:
     * e.g., https://example.com/remote.php/dav/calendars/<username>/
     */
    fun getCalendarsBaseUrl(): String {
        var cleanUrl = serverUrl.trim().trimEnd('/')
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            cleanUrl = "https://$cleanUrl"
        }
        val cleanUser = username.trim()

        return when {
            cleanUrl.endsWith("/remote.php/dav/calendars/$cleanUser") -> "$cleanUrl/"
            cleanUrl.endsWith("/remote.php/dav/calendars") -> "$cleanUrl/$cleanUser/"
            cleanUrl.endsWith("/remote.php/dav") -> "$cleanUrl/calendars/$cleanUser/"
            else -> "$cleanUrl/remote.php/dav/calendars/$cleanUser/"
        }
    }

    /**
     * Resolves a relative or full href against the server root.
     */
    fun resolveUrl(href: String): String {
        if (href.startsWith("http://") || href.startsWith("https://")) {
            return href
        }
        var cleanServer = serverUrl.trim().trimEnd('/')
        if (!cleanServer.startsWith("http://") && !cleanServer.startsWith("https://")) {
            cleanServer = "https://$cleanServer"
        }
        // Extract protocol + host (and port)
        val uri = java.net.URI(cleanServer)
        val hostRoot = "${uri.scheme}://${uri.authority}"
        val cleanHref = if (href.startsWith("/")) href else "/$href"
        return "$hostRoot$cleanHref"
    }
}
