package com.producto.timer.nextcloud

import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.xml.parsers.DocumentBuilderFactory

data class CalendarInfo(
    val name: String,
    val href: String
)

object CalDavParser {

    /**
     * Parses the PROPFIND XML response and returns calendars supporting VTODO components.
     */
    fun parseCalendars(xml: String): List<CalendarInfo> {
        val result = mutableListOf<CalendarInfo>()
        val cleanXml = xml.trim()
        if (cleanXml.isEmpty()) return result
        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(InputSource(StringReader(cleanXml)))
            doc.documentElement.normalize()

            val responses = doc.getElementsByTagNameNS("*", "response")
            for (i in 0 until responses.length) {
                val respElem = responses.item(i) as? Element ?: continue

                // Check HTTP status for this response
                val statusNodes = respElem.getElementsByTagNameNS("*", "status")
                if (statusNodes.length > 0) {
                    val statusText = statusNodes.item(0).textContent.orEmpty()
                    if (!statusText.contains("200")) continue
                }

                val hrefNodes = respElem.getElementsByTagNameNS("*", "href")
                if (hrefNodes.length == 0) continue
                val href = hrefNodes.item(0).textContent.trim()

                // Look for displayname
                var displayName = ""
                val nameNodes = respElem.getElementsByTagNameNS("*", "displayname")
                if (nameNodes.length > 0) {
                    displayName = nameNodes.item(0).textContent.trim()
                }
                if (displayName.isEmpty()) {
                    displayName = href.trimEnd('/').substringAfterLast('/')
                }

                // Check if this resource supports VTODO
                val compNodes = respElem.getElementsByTagNameNS("*", "comp")
                var supportsVTodo = false
                for (c in 0 until compNodes.length) {
                    val compElem = compNodes.item(c) as? Element ?: continue
                    val nameAttr = compElem.getAttribute("name")
                    if (nameAttr.equals("VTODO", ignoreCase = true)) {
                        supportsVTodo = true
                        break
                    }
                }

                // If no supported-calendar-component-set tag is present, check resourcetype contains calendar
                if (!supportsVTodo && compNodes.length == 0) {
                    val resTypes = respElem.getElementsByTagNameNS("*", "calendar")
                    if (resTypes.length > 0) {
                        supportsVTodo = true
                    }
                }

                if (supportsVTodo && href.isNotEmpty()) {
                    result.add(CalendarInfo(name = displayName, href = href))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("CalDavParser", "Error parsing calendars XML", e)
        }
        return result
    }

    /**
     * Parses the REPORT XML response containing VCALENDAR/VTODO items.
     */
    fun parseTasks(xml: String, calendarName: String, calendarHref: String): List<NextcloudTask> {
        val result = mutableListOf<NextcloudTask>()
        val cleanXml = xml.trim()
        if (cleanXml.isEmpty()) return result
        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(InputSource(StringReader(cleanXml)))
            doc.documentElement.normalize()

            val responses = doc.getElementsByTagNameNS("*", "response")
            for (i in 0 until responses.length) {
                val respElem = responses.item(i) as? Element ?: continue

                var href = ""
                val hrefNodes = respElem.getElementsByTagNameNS("*", "href")
                if (hrefNodes.length > 0) {
                    href = hrefNodes.item(0).textContent.trim()
                }

                var etag: String? = null
                val etagNodes = respElem.getElementsByTagNameNS("*", "getetag")
                if (etagNodes.length > 0) {
                    etag = etagNodes.item(0).textContent.trim().trim('"', '\'')
                }

                val dataNodes = respElem.getElementsByTagNameNS("*", "calendar-data")
                if (dataNodes.length == 0) continue
                val calData = dataNodes.item(0).textContent ?: continue

                val tasksFromData = parseVTodo(
                    rawCalendarData = calData,
                    calendarName = calendarName,
                    calendarHref = calendarHref,
                    taskHref = href,
                    etag = etag
                )
                result.addAll(tasksFromData)
            }
        } catch (e: Exception) {
            android.util.Log.e("CalDavParser", "Error parsing tasks XML", e)
        }
        return result
    }

    /**
     * Parses RFC 5545 VTODO blocks inside calendar data.
     */
    fun parseVTodo(
        rawCalendarData: String,
        calendarName: String,
        calendarHref: String,
        taskHref: String,
        etag: String?
    ): List<NextcloudTask> {
        val tasks = mutableListOf<NextcloudTask>()

        // 1. Unfold lines (RFC 5545 §3.1: lines starting with space or tab are continuation of previous line)
        val unfolded = rawCalendarData.replace(Regex("(\\r?\\n)+[ \\t]"), "")

        // 2. Extract VTODO blocks
        val todoRegex = Regex("BEGIN:VTODO([\\s\\S]*?)END:VTODO", RegexOption.IGNORE_CASE)
        val matches = todoRegex.findAll(unfolded)

        for (match in matches) {
            val block = match.groupValues[1]
            val lines = block.split(Regex("\\r?\\n"))

            var uid: String? = null
            var summary: String? = null
            var status: String? = null
            var dueDate: String? = null
            var priority: Int? = null

            for (line in lines) {
                val trimmed = line.trim()
                when {
                    trimmed.startsWith("UID:", ignoreCase = true) -> {
                        uid = trimmed.substringAfter(':').trim()
                    }
                    trimmed.startsWith("SUMMARY", ignoreCase = true) -> {
                        summary = unescapeIcsText(trimmed.substringAfter(':'))
                    }
                    trimmed.startsWith("STATUS:", ignoreCase = true) -> {
                        status = trimmed.substringAfter(':').trim().uppercase(Locale.ROOT)
                    }
                    trimmed.startsWith("DUE", ignoreCase = true) -> {
                        dueDate = formatIcsDate(trimmed.substringAfter(':').trim())
                    }
                    trimmed.startsWith("PRIORITY:", ignoreCase = true) -> {
                        priority = trimmed.substringAfter(':').trim().toIntOrNull()
                    }
                }
            }

            if (!uid.isNullOrBlank() && !summary.isNullOrBlank()) {
                val isCompleted = status == "COMPLETED" || status == "CANCELLED"
                tasks.add(
                    NextcloudTask(
                        uid = uid,
                        summary = summary,
                        isCompleted = isCompleted,
                        dueDate = dueDate,
                        priority = priority,
                        calendarName = calendarName,
                        calendarHref = calendarHref,
                        taskHref = taskHref,
                        etag = etag
                    )
                )
            }
        }
        return tasks
    }

    /**
     * Unescapes iCalendar escaped text (RFC 5545 §3.3.11).
     */
    private fun unescapeIcsText(text: String): String {
        return text
            .replace("\\n", "\n", ignoreCase = true)
            .replace("\\,", ",")
            .replace("\\;", ";")
            .replace("\\\\", "\\")
            .trim()
    }

    /**
     * Parses standard iCalendar DATE / DATE-TIME into a human-readable string.
     * e.g. 20260910T153000Z or 20260910
     */
    private fun formatIcsDate(raw: String): String {
        return try {
            val clean = raw.trim()
            if (clean.length == 8) { // YYYYMMDD
                val year = clean.substring(0, 4)
                val month = clean.substring(4, 6)
                val day = clean.substring(6, 8)
                "$year-$month-$day"
            } else if (clean.contains("T")) {
                val datePart = clean.substringBefore("T")
                if (datePart.length == 8) {
                    val year = datePart.substring(0, 4)
                    val month = datePart.substring(4, 6)
                    val day = datePart.substring(6, 8)
                    "$year-$month-$day"
                } else {
                    clean
                }
            } else {
                clean
            }
        } catch (_: Exception) {
            raw
        }
    }

    /**
     * Updates an existing iCalendar VCALENDAR string to mark the task completed.
     */
    fun markTaskCompletedInIcs(originalIcs: String): String {
        val now = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())

        val statusCompleted = "STATUS:COMPLETED\r\nCOMPLETED:$now\r\nPERCENT-COMPLETE:100"

        return if (originalIcs.contains(Regex("STATUS:[^\\r\\n]*", RegexOption.IGNORE_CASE))) {
            originalIcs.replace(
                Regex("STATUS:[^\\r\\n]*", RegexOption.IGNORE_CASE),
                statusCompleted
            )
        } else {
            // Append before END:VTODO
            originalIcs.replace(
                Regex("END:VTODO", RegexOption.IGNORE_CASE),
                "$statusCompleted\r\nEND:VTODO"
            )
        }
    }
}
