package com.sf.tadami.data.backup.models

import com.sf.tadami.domain.history.History
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import java.util.Date

@Serializable
data class BackupHistory @OptIn(ExperimentalSerializationApi::class) constructor(
    @ProtoNumber(1) var url: String,
    @ProtoNumber(2) var seenAt: Long,
) {
    fun getHistoryImpl(): History {
        return History.create().copy(
            seenAt = Date(seenAt),
        )
    }
}