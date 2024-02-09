package com.sf.tadami.data.backup.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
data class BackupSource @OptIn(ExperimentalSerializationApi::class) constructor(
    @ProtoNumber(1) var name: String = "",
    @ProtoNumber(2) var sourceId: Long,
)