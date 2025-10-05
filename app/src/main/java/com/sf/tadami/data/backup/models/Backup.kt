package com.sf.tadami.data.backup.models

import com.sf.tadami.BuildConfig
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Backup(
    @ProtoNumber(1) val backupAnime: List<BackupAnime>,
    @ProtoNumber(100) var backupPreferences: List<BackupPreference> = emptyList(),
    @ProtoNumber(101) var backupSources: List<BackupSource> = emptyList(),
    @ProtoNumber(102) var backupSourcePreferences: List<BackupSourcePreferences> = emptyList(),
)

object BackupUtils {
    val filenameRegex = """${BuildConfig.APPLICATION_ID}_\d+-\d+-\d+_\d+-\d+.tadabk""".toRegex()

    fun getFilename(): String {
        val date = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())
        return "${BuildConfig.APPLICATION_ID}_$date.tadabk"
    }
}