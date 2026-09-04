package com.producto.timer

import com.producto.timer.nextcloud.CalDavParser
import com.producto.timer.nextcloud.NextcloudConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalDavParserTest {

    @Test
    fun parseCalendars_extractsVTodoCalendars() {
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <d:multistatus xmlns:d="DAV:" xmlns:cal="urn:ietf:params:xml:ns:caldav">
              <d:response>
                <d:href>/remote.php/dav/calendars/john/personal/</d:href>
                <d:propstat>
                  <d:prop>
                    <d:displayname>Personal Tasks</d:displayname>
                    <d:resourcetype><d:collection/><cal:calendar/></d:resourcetype>
                    <cal:supported-calendar-component-set>
                      <cal:comp name="VEVENT"/>
                      <cal:comp name="VTODO"/>
                    </cal:supported-calendar-component-set>
                  </d:prop>
                  <d:status>HTTP/1.1 200 OK</d:status>
                </d:propstat>
              </d:response>
              <d:response>
                <d:href>/remote.php/dav/calendars/john/events-only/</d:href>
                <d:propstat>
                  <d:prop>
                    <d:displayname>Events Only</d:displayname>
                    <cal:supported-calendar-component-set>
                      <cal:comp name="VEVENT"/>
                    </cal:supported-calendar-component-set>
                  </d:prop>
                  <d:status>HTTP/1.1 200 OK</d:status>
                </d:propstat>
              </d:response>
            </d:multistatus>
        """.trimIndent()

        val calendars = CalDavParser.parseCalendars(xml)
        assertEquals(1, calendars.size)
        assertEquals("Personal Tasks", calendars[0].name)
        assertEquals("/remote.php/dav/calendars/john/personal/", calendars[0].href)
    }

    @Test
    fun parseTasks_extractsOpenTasksWithProperties() {
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <d:multistatus xmlns:d="DAV:" xmlns:cal="urn:ietf:params:xml:ns:caldav">
              <d:response>
                <d:href>/remote.php/dav/calendars/john/personal/task1.ics</d:href>
                <d:propstat>
                  <d:prop>
                    <d:getetag>"etag123"</d:getetag>
                    <cal:calendar-data>BEGIN:VCALENDAR
VERSION:2.0
BEGIN:VTODO
UID:uid-abc-123
SUMMARY:Write architecture design\, review with team
STATUS:NEEDS-ACTION
PRIORITY:1
DUE:20261015T120000Z
END:VTODO
END:VCALENDAR</cal:calendar-data>
                  </d:prop>
                  <d:status>HTTP/1.1 200 OK</d:status>
                </d:propstat>
              </d:response>
            </d:multistatus>
        """.trimIndent()

        val tasks = CalDavParser.parseTasks(xml, "Personal", "/remote.php/dav/calendars/john/personal/")
        assertEquals(1, tasks.size)
        val task = tasks[0]
        assertEquals("uid-abc-123", task.uid)
        assertEquals("Write architecture design, review with team", task.summary)
        assertFalse(task.isCompleted)
        assertEquals(1, task.priority)
        assertEquals("2026-10-15", task.dueDate)
        assertEquals("etag123", task.etag)
    }

    @Test
    fun markTaskCompletedInIcs_updatesStatusProperly() {
        val original = """
            BEGIN:VCALENDAR
            BEGIN:VTODO
            UID:task-99
            SUMMARY:Sample Task
            STATUS:NEEDS-ACTION
            END:VTODO
            END:VCALENDAR
        """.trimIndent()

        val updated = CalDavParser.markTaskCompletedInIcs(original)
        assertTrue(updated.contains("STATUS:COMPLETED"))
        assertTrue(updated.contains("PERCENT-COMPLETE:100"))
        assertTrue(updated.contains("COMPLETED:"))
    }

    @Test
    fun nextcloudConfig_normalizesUrls() {
        val config1 = NextcloudConfig("https://cloud.example.com", "john", "secret")
        assertEquals(
            "https://cloud.example.com/remote.php/dav/calendars/john/",
            config1.getCalendarsBaseUrl()
        )

        val config2 = NextcloudConfig("cloud.example.com/remote.php/dav", "john", "secret")
        assertEquals(
            "https://cloud.example.com/remote.php/dav/calendars/john/",
            config2.getCalendarsBaseUrl()
        )

        assertEquals(
            "https://cloud.example.com/remote.php/dav/calendars/john/tasks/task1.ics",
            config1.resolveUrl("/remote.php/dav/calendars/john/tasks/task1.ics")
        )
    }
}
