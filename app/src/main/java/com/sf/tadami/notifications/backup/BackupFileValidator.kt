package com.sf.tadami.notifications.backup

import android.content.Context
import android.net.Uri
import com.sf.tadami.R
import com.sf.tadami.data.backup.BackupUtil
import com.sf.tadami.ui.tabs.animesources.AnimeSourcesManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class BackupFileValidator(
    private val sourceManager: AnimeSourcesManager = Injekt.get(),
) {

    /**
     * Checks for critical backup file data.
     *
     * @throws Exception if anime cannot be found.
     * @return List of missing sources or missing trackers.
     */
    fun validate(context: Context, uri: Uri) {
        val backup = try {
            BackupUtil.decodeBackup(context, uri)
        } catch (e: Exception) {
            throw IllegalStateException(e)
        }

        if (backup.backupAnime.isEmpty()) {
            throw IllegalStateException(context.getString(R.string.invalid_backup_file_missing_anime))
        }
    }
}
