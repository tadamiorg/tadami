package com.sf.animescraper.network.scraping.dto.details

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Keep
@Serializable
@Parcelize
data class DetailsEpisode(
    var id: Long? = null,
    var url: String,
    val name: String? = null,
    val date: String? = null,
    var seen: Boolean
) : Parcelable

@Keep
@Serializable
@Parcelize
data class DetailsEpisodeList(
    val episodes: List<DetailsEpisode>
) : Parcelable
