package com.producto.timer.nextcloud

/**
 * Represents a task retrieved from Nextcloud Tasks via CalDAV (RFC 5545 VTODO).
 */
data class NextcloudTask(
    val uid: String,
    val summary: String,
    val isCompleted: Boolean = false,
    val dueDate: String? = null,
    val priority: Int? = null,
    val calendarName: String? = null,
    val calendarHref: String? = null,
    val taskHref: String? = null,
    val etag: String? = null
)
