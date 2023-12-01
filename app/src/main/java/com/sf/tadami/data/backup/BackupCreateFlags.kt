package com.sf.tadami.data.backup

internal object BackupCreateFlags {
    const val BACKUP_EPISODE = 0x00000001
    const val BACKUP_APP_PREFS = 0x00000002
    const val BACKUP_DEFAULT_ANIME = 0x00000004

    val SET = setOf(
        BACKUP_EPISODE,
        BACKUP_APP_PREFS,
        BACKUP_DEFAULT_ANIME
    )

    const val AutomaticDefaults =
        BACKUP_DEFAULT_ANIME or
                BACKUP_EPISODE or
                BACKUP_APP_PREFS

}