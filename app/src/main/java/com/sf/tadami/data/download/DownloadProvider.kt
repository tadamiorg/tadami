package com.sf.tadami.data.download

import android.content.Context
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.hippo.unifile.UniFile
import com.sf.tadami.preferences.backup.BackupPreferences
import com.sf.tadami.source.online.Source
import com.sf.tadami.utils.getPreferencesGroup
import kotlinx.coroutines.runBlocking
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class DownloadProvider(
    val context: Context,
    val dataStore : DataStore<Preferences> = Injekt.get()
) {
    private fun getBaseDirPref() : String{
        return runBlocking {
            dataStore.getPreferencesGroup(BackupPreferences).autoBackupFolder
        }
    }
    private var baseDir: UniFile? = getBaseDir(getBaseDirPref())

    private val downloadsDir: UniFile?
        get() = getDownloadsDirectory()

    /**
     * Returns the download directory for a source if it exists.
     *
     * @param source the source to query.
     */
    fun findSourceDir(source: Source): UniFile? {
        return downloadsDir?.findFile(getSourceDirName(source), true)
    }

    /**
     * Returns the download directory name for a source.
     *
     * @param source the source to query.
     */
    fun getSourceDirName(source: Source): String {
        return buildValidFilename(source.toString())
    }
    /**
     * Mutate the given filename to make it valid for a FAT filesystem,
     * replacing any invalid characters with "_". This method doesn't allow hidden files (starting
     * with a dot), but you can manually add it later.
     */
    fun buildValidFilename(origName: String): String {
        val name = origName.trim('.', ' ')
        if (name.isEmpty()) {
            return "(invalid)"
        }
        val sb = StringBuilder(name.length)
        name.forEach { c ->
            if (isValidFatFilenameChar(c)) {
                sb.append(c)
            } else {
                sb.append('_')
            }
        }
        // Even though vfat allows 255 UCS-2 chars, we might eventually write to
        // ext4 through a FUSE layer, so use that limit minus 15 reserved characters.
        return sb.toString().take(240)
    }

    /**
     * Returns true if the given character is a valid filename character, false otherwise.
     */
    private fun isValidFatFilenameChar(c: Char): Boolean {
        if (0x00.toChar() <= c && c <= 0x1f.toChar()) {
            return false
        }
        return when (c) {
            '"', '*', '/', ':', '<', '>', '?', '\\', '|', 0x7f.toChar() -> false
            else -> true
        }
    }

    fun getDownloadsDirectory(): UniFile? {
        return baseDir?.createDirectory(DOWNLOADS_PATH)
    }

    private fun getBaseDir(uri: String): UniFile? {
        return UniFile.fromUri(context, uri.toUri())
            .takeIf { it?.exists() == true }
    }


    companion object{
        const val DOWNLOADS_PATH = "downloads"
    }

}