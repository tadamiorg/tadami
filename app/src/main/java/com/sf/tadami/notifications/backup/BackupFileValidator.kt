package com.sf.tadami.notifications.backup

import android.content.Context
import android.net.Uri
import com.sf.tadami.R
import com.sf.tadami.data.backup.BackupDecoder
import com.sf.tadami.ui.tabs.browse.SourceManager
import kotlinx.serialization.ExperimentalSerializationApi
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class BackupFileValidator(
    private val sourceManager: SourceManager = Injekt.get(),
) {

    /**
     * Checks for critical backup file data.
     *
     * @throws Exception if anime cannot be found.
     * @return List of missing sources or missing trackers.
     */
    @OptIn(ExperimentalSerializationApi::class)
    fun validate(context: Context, uri: Uri): Results  {
        val backup = try {
            BackupDecoder(context).decode(uri)
        } catch (e: Exception) {
            throw IllegalStateException(e)
        }

        val sources = backup.backupSources.associate { it.sourceId to it.name }
        val missingSources = sources
            .filter { sourceManager.get(it.key) == null }
            .entries.map {
                sourceManager.getOrStub(it.key,it.value).toString()
            }
            .distinct()
            .sorted()

        if (backup.backupAnime.isEmpty()) {
            throw IllegalStateException(context.getString(R.string.invalid_backup_file_missing_anime))
        }
        return Results(missingSources)
    }

    data class Results(
        val missingSources: List<String>
    )
}
