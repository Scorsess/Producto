package com.producto.timer.nextcloud

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class NextcloudCalDavClient(
    private val config: NextcloudConfig,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
) {

    private val xmlMediaType = "application/xml; charset=utf-8".toMediaType()
    private val icsMediaType = "text/calendar; charset=utf-8".toMediaType()

    /**
     * Tests connectivity and credentials with the Nextcloud server.
     * Returns the count of found task calendars on success.
     */
    suspend fun testConnection(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val calendars = getTaskCalendars()
            Result.success(calendars.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches all open/pending tasks across all available task calendars.
     */
    suspend fun fetchOpenTasks(): Result<List<NextcloudTask>> = withContext(Dispatchers.IO) {
        try {
            val calendars = getTaskCalendars()
            if (calendars.isEmpty()) {
                return@withContext Result.success(emptyList())
            }

            val allTasks = mutableListOf<NextcloudTask>()
            for (cal in calendars) {
                val calTasks = fetchTasksFromCalendar(cal)
                allTasks.addAll(calTasks)
            }

            // Filter out completed tasks and sort by priority (higher priority first), then summary
            val openTasks = allTasks.filter { !it.isCompleted }
                .sortedWith(
                    compareBy<NextcloudTask> { task ->
                        // In RFC 5545, 1 is highest priority, 9 is lowest, 0 or null is undefined
                        when (val p = task.priority) {
                            null, 0 -> 99
                            else -> p
                        }
                    }.thenBy { it.summary.lowercase() }
                )

            Result.success(openTasks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Marks a specific task as COMPLETED in Nextcloud.
     */
    suspend fun completeTask(task: NextcloudTask): Result<Unit> = withContext(Dispatchers.IO) {
        val taskHref = task.taskHref ?: return@withContext Result.failure(
            IllegalArgumentException("Task has no valid URL href")
        )
        val taskUrl = config.resolveUrl(taskHref)

        try {
            // 1. GET current calendar item
            val getRequest = Request.Builder()
                .url(taskUrl)
                .header("Authorization", config.basicAuthHeader())
                .header("Accept", "text/calendar")
                .get()
                .build()

            val response = client.newCall(getRequest).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    IOException("Failed to fetch task for update: HTTP ${response.code}")
                )
            }

            val originalIcs = response.body?.string().orEmpty()
            val currentEtag = response.header("ETag") ?: task.etag
            val updatedIcs = CalDavParser.markTaskCompletedInIcs(originalIcs)

            // 2. PUT updated calendar item
            val putBuilder = Request.Builder()
                .url(taskUrl)
                .header("Authorization", config.basicAuthHeader())
                .header("Content-Type", "text/calendar; charset=utf-8")
                .put(updatedIcs.toRequestBody(icsMediaType))

            if (!currentEtag.isNullOrBlank()) {
                putBuilder.header("If-Match", currentEtag)
            }

            val putResponse = client.newCall(putBuilder.build()).execute()
            if (putResponse.isSuccessful || putResponse.code == 204 || putResponse.code == 201) {
                Result.success(Unit)
            } else {
                Result.failure(IOException("Failed to mark task completed: HTTP ${putResponse.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Discovers all calendar collections that support VTODO.
     */
    private fun getTaskCalendars(): List<CalendarInfo> {
        val baseUrl = config.getCalendarsBaseUrl()

        val propfindXml = """
            <?xml version="1.0" encoding="utf-8" ?>
            <d:propfind xmlns:d="DAV:" xmlns:cal="urn:ietf:params:xml:ns:caldav">
              <d:prop>
                <d:displayname />
                <d:resourcetype />
                <cal:supported-calendar-component-set />
              </d:prop>
            </d:propfind>
        """.trimIndent()

        val request = Request.Builder()
            .url(baseUrl)
            .header("Authorization", config.basicAuthHeader())
            .header("Depth", "1")
            .method("PROPFIND", propfindXml.toRequestBody(xmlMediaType))
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful && response.code != 207) {
            throw IOException("Failed to query Nextcloud calendars: HTTP ${response.code} (${response.message})")
        }

        val xmlBody = response.body?.string().orEmpty()
        return CalDavParser.parseCalendars(xmlBody)
    }

    /**
     * Queries a specific calendar collection for VTODO entries using REPORT.
     */
    private fun fetchTasksFromCalendar(calendar: CalendarInfo): List<NextcloudTask> {
        val calendarUrl = config.resolveUrl(calendar.href)

        // Query for open tasks (STATUS != COMPLETED)
        val reportXml = """
            <?xml version="1.0" encoding="utf-8" ?>
            <c:calendar-query xmlns:d="DAV:" xmlns:c="urn:ietf:params:xml:ns:caldav">
              <d:prop>
                <d:getetag />
                <c:calendar-data />
              </d:prop>
              <c:filter>
                <c:comp-filter name="VCALENDAR">
                  <c:comp-filter name="VTODO">
                    <c:prop-filter name="STATUS">
                      <c:text-match collation="i;ascii-casemap" negate-condition="yes">COMPLETED</c:text-match>
                    </c:prop-filter>
                  </c:comp-filter>
                </c:comp-filter>
              </c:filter>
            </c:calendar-query>
        """.trimIndent()

        val request = Request.Builder()
            .url(calendarUrl)
            .header("Authorization", config.basicAuthHeader())
            .header("Depth", "1")
            .method("REPORT", reportXml.toRequestBody(xmlMediaType))
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful && response.code != 207) {
            return emptyList()
        }

        val xmlBody = response.body?.string().orEmpty()
        return CalDavParser.parseTasks(
            xml = xmlBody,
            calendarName = calendar.name,
            calendarHref = calendar.href
        )
    }
}
