# Nextcloud Tasks Integration Guide

Producto features a standalone, direct **CalDAV** client that synchronizes with Nextcloud Tasks without requiring external sync services (such as DAVx⁵ or third-party apps).

---

## 1. Overview

Nextcloud Tasks manages to-do items using the standard iCalendar `VTODO` format specified in **RFC 5545** and **RFC 4791** (CalDAV). Tasks are grouped into calendar collections hosted on your Nextcloud instance.

Producto connects directly to your instance over HTTPS using HTTP Basic Authentication with dedicated Nextcloud App Tokens, discovers which calendars contain task components, queries for pending/open items, and lets you check off completed items directly from the app.

---

## 2. Setup & Authentication

### Step 1: Generate a Nextcloud App Password
For security, avoid using your primary account password. Instead:
1. Log into your Nextcloud web interface.
2. Click your user avatar in the top right and select **Personal Settings**.
3. Under the **Security** section on the left, find **Devices & sessions**.
4. Type `Producto Timer` in the **App name** box and click **Create new app password**.
5. Copy the generated password token.

### Step 2: Configure in Producto
1. Tap the checkbox icon (**`☑`**) in the top-left corner of the timer screen.
2. Enter:
   * **Server URL**: The base URL of your Nextcloud instance (e.g., `https://cloud.yourdomain.com`).
   * **Username**: Your Nextcloud username.
   * **App Password / Token**: The token generated in Step 1.
3. Tap **Connect**.
4. Producto will validate your credentials, perform calendar discovery, and load your open tasks.

---

## 3. CalDAV Protocol Implementation

### A. Calendar Discovery (`PROPFIND`)
Producto queries the CalDAV calendar root:
```http
PROPFIND /remote.php/dav/calendars/<username>/ HTTP/1.1
Depth: 1
Authorization: Basic <base64(username:password)>
Content-Type: application/xml; charset=utf-8

<?xml version="1.0" encoding="utf-8" ?>
<d:propfind xmlns:d="DAV:" xmlns:cal="urn:ietf:params:xml:ns:caldav">
  <d:prop>
    <d:displayname />
    <d:resourcetype />
    <cal:supported-calendar-component-set />
  </d:prop>
</d:propfind>
```
The response is parsed to identify collections whose `<cal:supported-calendar-component-set>` contains `<cal:comp name="VTODO"/>`.

### B. Querying Open Tasks (`REPORT`)
For each task calendar collection, a `REPORT` request is issued:
```http
REPORT /remote.php/dav/calendars/<username>/<calendar-id>/ HTTP/1.1
Depth: 1
Authorization: Basic <base64(username:password)>
Content-Type: application/xml; charset=utf-8

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
```
The query uses `negate-condition="yes"` on `COMPLETED` so that tasks without an explicit status (`NEEDS-ACTION`, `IN-PROCESS`, or unspecified) are retrieved.

### C. Marking Tasks Complete (`PUT`)
When a task is marked complete:
1. The original task iCalendar data is retrieved via `GET`.
2. The `STATUS` property is updated to `STATUS:COMPLETED`, and `COMPLETED:<timestamp>` and `PERCENT-COMPLETE:100` are added.
3. The modified payload is sent back via `PUT` with an `If-Match: <etag>` header to prevent concurrency conflicts.

---

## 4. UI / UX Workflow

* **Active Focus Target**: Tap any task in the dialog to designate it as the active focus goal. A badge (`🎯 Task Name`) is displayed above the countdown ring.
* **Inline Task Completion**: Tap the circle next to any task in the list to complete it on Nextcloud.
* **Post-Pomodoro Prompt**: When a Focus session completes, a button appears under the timer:
  `✓ Complete "<task>" on Nextcloud`
  allowing you to check off your accomplishment with one tap.

---

## 5. Security & Privacy

* **Direct Point-to-Point**: Connection is strictly between your Android device and your Nextcloud server. No intermediate servers or telemetry.
* **Local Storage**: Server URL, username, and token are stored strictly in local private app preferences (`MODE_PRIVATE`).
* **Instant Disconnect**: Tapping **Disconnect** completely clears all stored credentials, cached tasks, and active targets from the device.
