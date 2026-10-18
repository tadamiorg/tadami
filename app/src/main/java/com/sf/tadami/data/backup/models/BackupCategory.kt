package com.sf.tadami.data.backup.models

import com.sf.tadami.domain.category.Category
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

// The anime name their categories by [id], the id they had in the install the backup comes from
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class BackupCategory(
    @ProtoNumber(1) var name: String,
    @ProtoNumber(2) var order: Long = 0,
    @ProtoNumber(3) var id: Long = 0,
    @ProtoNumber(100) var flags: Long = 0,
) {
    companion object {
        fun copyFrom(category: Category): BackupCategory {
            return BackupCategory(
                name = category.name,
                order = category.order,
                id = category.id,
                flags = category.flags
            )
        }
    }
}
