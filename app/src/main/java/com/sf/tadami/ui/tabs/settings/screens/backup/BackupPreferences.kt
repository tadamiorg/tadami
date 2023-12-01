package com.sf.tadami.ui.tabs.settings.screens.backup

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sf.tadami.data.providers.AndroidFoldersProvider
import com.sf.tadami.ui.tabs.settings.model.CustomPreferences
import com.sf.tadami.ui.tabs.settings.model.CustomPreferencesIdentifier
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

data class BackupPreferences(
    val autoBackupMaxFiles: Int,
    val autoBackupInterval: Int,
    val autoBackupFolder: String,
    val autoBackupLastTimestamp : Long
) : CustomPreferencesIdentifier {

    object AutoBackupIntervalItems {
        const val DISABLED = 0
        const val DAILY = 24
        const val DAILY_2 = 48
        const val DAILY_3 = 72
        const val WEEKLY = 168
    }

    object AutoBackupMaxFiles {
        const val TWO = 2
        const val THREE = 3
        const val FOUR = 4
        const val FIVE = 5
    }

    companion object : CustomPreferences<BackupPreferences> {
        private val AUTO_BACKUP_MAX_FILES = intPreferencesKey("auto_backup_max_files")
        private val AUTO_BACKUP_INTERVAL = intPreferencesKey("auto_backup_interval")
        private val AUTO_BACKUP_FOLDER = stringPreferencesKey("auto_backup_folder")
        private val AUTO_BACKUP_LAST_TIMESTAMP = longPreferencesKey("auto_backup_last_timestamp")

        override fun transform(preferences: Preferences): BackupPreferences {
            val foldersProviders: AndroidFoldersProvider = Injekt.get()
            return BackupPreferences(
                autoBackupMaxFiles = preferences[AUTO_BACKUP_MAX_FILES] ?: AutoBackupMaxFiles.TWO,
                autoBackupInterval = preferences[AUTO_BACKUP_INTERVAL] ?: AutoBackupIntervalItems.DISABLED,
                autoBackupFolder = preferences[AUTO_BACKUP_FOLDER] ?: foldersProviders.backupPath(),
                autoBackupLastTimestamp = preferences[AUTO_BACKUP_LAST_TIMESTAMP] ?: 0L
            )
        }

        override fun setPrefs(newValue: BackupPreferences, preferences: MutablePreferences) {
            preferences[AUTO_BACKUP_MAX_FILES] = newValue.autoBackupMaxFiles
            preferences[AUTO_BACKUP_INTERVAL] = newValue.autoBackupInterval
            preferences[AUTO_BACKUP_FOLDER] = newValue.autoBackupFolder
            preferences[AUTO_BACKUP_LAST_TIMESTAMP] = newValue.autoBackupLastTimestamp
        }
    }
}