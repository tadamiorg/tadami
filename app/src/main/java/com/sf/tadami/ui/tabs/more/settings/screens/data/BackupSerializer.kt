package com.sf.tadami.ui.tabs.more.settings.screens.data

import com.sf.tadami.data.backup.models.Backup
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializer

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = Backup::class)
object BackupSerializer