package com.sf.tadami.preferences.backup

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.sf.tadami.preferences.model.CustomPreferences
import com.sf.tadami.preferences.model.CustomPreferencesIdentifier

data class BackupPreferences(
    val autoBackupInterval: Int,
    val autoBackupLastTimestamp : Long
) : CustomPreferencesIdentifier {

    object AutoBackupIntervalItems {
        const val DISABLED = 0
        const val DAILY = 24
        const val DAILY_2 = 48
        const val DAILY_3 = 72
        const val WEEKLY = 168
    }

    companion object : CustomPreferences<BackupPreferences> {
        private val AUTO_BACKUP_INTERVAL = intPreferencesKey("auto_backup_interval")
        val AUTO_BACKUP_LAST_TIMESTAMP = longPreferencesKey("auto_backup_last_timestamp")
        // TODO remove MAX files from datastore migrations


        override fun transform(preferences: Preferences): BackupPreferences {
            return BackupPreferences(
                autoBackupInterval = preferences[AUTO_BACKUP_INTERVAL] ?: AutoBackupIntervalItems.DISABLED,
                autoBackupLastTimestamp = preferences[AUTO_BACKUP_LAST_TIMESTAMP] ?: 0L
            )
        }

        override fun setPrefs(newValue: BackupPreferences, preferences: MutablePreferences) {
            preferences[AUTO_BACKUP_INTERVAL] = newValue.autoBackupInterval
            preferences[AUTO_BACKUP_LAST_TIMESTAMP] = newValue.autoBackupLastTimestamp
        }
    }
}