package com.sf.tadami.data.backup.models

import com.sf.tadami.domain.history.History
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import java.util.Date
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class BackupHistory(
    @ProtoNumber(1) var url: String,
    @ProtoNumber(2) var seenAt: Long,
) {
    fun getHistoryImpl(): History {
        return History.create().copy(
            seenAt = Date(seenAt),
        )
    }
}