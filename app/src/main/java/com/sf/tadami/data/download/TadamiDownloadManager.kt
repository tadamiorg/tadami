package com.sf.tadami.data.download

import android.util.Log
import com.sf.tadami.source.Source
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class TadamiDownloadManager(
    private val provider: DownloadProvider = Injekt.get(),
) {
    /**
     * Renames source download folder
     *
     * @param oldSource the old source.
     * @param newSource the new source.
     */
    fun renameSource(oldSource: Source, newSource: Source) {
        val oldFolder = provider.findSourceDir(oldSource) ?: return
        val newName = provider.getSourceDirName(newSource)

        if (oldFolder.name == newName) return

        val capitalizationChanged = oldFolder.name.equals(newName, ignoreCase = true)
        if (capitalizationChanged) {
            val tempName = newName + TMP_DIR_SUFFIX
            if (oldFolder.renameTo(tempName).not()) {
                Log.d("RenameSource", "Failed to rename source download folder: ${oldFolder.name}")
                return
            }
        }

        if (oldFolder.renameTo(newName).not()) {
            Log.d("RenameSource", "Failed to rename source download folder: ${oldFolder.name}")
        }
    }

    companion object {
        const val TMP_DIR_SUFFIX = "_tmp"
    }

}