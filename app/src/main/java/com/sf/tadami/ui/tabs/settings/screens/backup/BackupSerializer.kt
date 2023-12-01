package com.sf.tadami.ui.tabs.settings.screens.backup

import com.sf.tadami.data.backup.models.Backup
import kotlinx.serialization.Serializer

@Serializer(forClass = Backup::class)
object BackupSerializer