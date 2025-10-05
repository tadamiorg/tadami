package com.sf.tadami.data.backup.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class BackupPreference(
    @ProtoNumber(1) val key: String,
    @ProtoNumber(2) val value: PreferenceValue,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class BackupSourcePreferences(
    @ProtoNumber(1) val sourceKey: Long,
    @ProtoNumber(2) val prefs: List<BackupPreference>,
)

@Serializable
sealed class PreferenceValue

@Serializable
data class IntPreferenceValue(val value: Int) : PreferenceValue()

@Serializable
data class LongPreferenceValue(val value: Long) : PreferenceValue()

@Serializable
data class FloatPreferenceValue(val value: Float) : PreferenceValue()

@Serializable
data class StringPreferenceValue(val value: String) : PreferenceValue()

@Serializable
data class BooleanPreferenceValue(val value: Boolean) : PreferenceValue()

@Serializable
data class StringSetPreferenceValue(val value: Set<String>) : PreferenceValue()
