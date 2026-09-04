package com.producto.timer.nextcloud

import android.content.Context
import androidx.core.content.edit

interface NextcloudPreferences {
    var serverUrl: String
    var username: String
    var appPassword: String
    var activeTaskUid: String?
    var activeTaskSummary: String?

    fun getConfig(): NextcloudConfig?
    fun saveConfig(config: NextcloudConfig)
    fun clearConfig()
    fun clearActiveTask()
}

class AndroidNextcloudPreferences(context: Context) : NextcloudPreferences {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "nextcloud_prefs"
        private const val KEY_SERVER_URL = "nc_server_url"
        private const val KEY_USERNAME = "nc_username"
        private const val KEY_APP_PASSWORD = "nc_app_password"
        private const val KEY_ACTIVE_TASK_UID = "nc_active_task_uid"
        private const val KEY_ACTIVE_TASK_SUMMARY = "nc_active_task_summary"
    }

    override var serverUrl: String
        get() = prefs.getString(KEY_SERVER_URL, "").orEmpty()
        set(value) = prefs.edit { putString(KEY_SERVER_URL, value) }

    override var username: String
        get() = prefs.getString(KEY_USERNAME, "").orEmpty()
        set(value) = prefs.edit { putString(KEY_USERNAME, value) }

    override var appPassword: String
        get() = prefs.getString(KEY_APP_PASSWORD, "").orEmpty()
        set(value) = prefs.edit { putString(KEY_APP_PASSWORD, value) }

    override var activeTaskUid: String?
        get() = prefs.getString(KEY_ACTIVE_TASK_UID, null)
        set(value) = prefs.edit { putString(KEY_ACTIVE_TASK_UID, value) }

    override var activeTaskSummary: String?
        get() = prefs.getString(KEY_ACTIVE_TASK_SUMMARY, null)
        set(value) = prefs.edit { putString(KEY_ACTIVE_TASK_SUMMARY, value) }

    override fun getConfig(): NextcloudConfig? {
        val server = serverUrl.trim()
        val user = username.trim()
        val pass = appPassword.trim()
        if (server.isEmpty() || user.isEmpty() || pass.isEmpty()) return null
        return NextcloudConfig(server, user, pass)
    }

    override fun saveConfig(config: NextcloudConfig) {
        prefs.edit {
            putString(KEY_SERVER_URL, config.serverUrl)
            putString(KEY_USERNAME, config.username)
            putString(KEY_APP_PASSWORD, config.appPassword)
        }
    }

    override fun clearConfig() {
        prefs.edit {
            remove(KEY_SERVER_URL)
            remove(KEY_USERNAME)
            remove(KEY_APP_PASSWORD)
            remove(KEY_ACTIVE_TASK_UID)
            remove(KEY_ACTIVE_TASK_SUMMARY)
        }
    }

    override fun clearActiveTask() {
        prefs.edit {
            remove(KEY_ACTIVE_TASK_UID)
            remove(KEY_ACTIVE_TASK_SUMMARY)
        }
    }
}
