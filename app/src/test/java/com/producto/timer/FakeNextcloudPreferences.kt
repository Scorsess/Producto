package com.producto.timer

import com.producto.timer.nextcloud.NextcloudConfig
import com.producto.timer.nextcloud.NextcloudPreferences

class FakeNextcloudPreferences(
    override var serverUrl: String = "",
    override var username: String = "",
    override var appPassword: String = "",
    override var activeTaskUid: String? = null,
    override var activeTaskSummary: String? = null
) : NextcloudPreferences {

    override fun getConfig(): NextcloudConfig? {
        if (serverUrl.isBlank() || username.isBlank() || appPassword.isBlank()) return null
        return NextcloudConfig(serverUrl, username, appPassword)
    }

    override fun saveConfig(config: NextcloudConfig) {
        serverUrl = config.serverUrl
        username = config.username
        appPassword = config.appPassword
    }

    override fun clearConfig() {
        serverUrl = ""
        username = ""
        appPassword = ""
        activeTaskUid = null
        activeTaskSummary = null
    }

    override fun clearActiveTask() {
        activeTaskUid = null
        activeTaskSummary = null
    }
}
