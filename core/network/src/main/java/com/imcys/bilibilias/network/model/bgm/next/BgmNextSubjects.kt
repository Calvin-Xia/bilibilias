package com.imcys.bilibilias.network.model.bgm.next

import com.imcys.bilibilias.network.model.bgm.BgmSubjectImages
import com.imcys.bilibilias.network.model.bgm.BgmSubjectRating
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class BgmNextSubject(
    @SerialName("airtime")
    val airtime: Airtime,
    @SerialName("collection")
    val collection: Map<String, Int>,
    @SerialName("eps")
    val eps: Int,
    @SerialName("id")
    val id: Long,
    @SerialName("info")
    val info: String,
    @SerialName("locked")
    val locked: Boolean,
    @SerialName("metaTags")
    val metaTags: List<String>,
    @SerialName("infobox")
    val infobox: List<BgmInfobox>,
    @SerialName("name")
    val name: String,
    @SerialName("nameCN")
    val nameCN: String,
    @SerialName("nsfw")
    val nsfw: Boolean,
    @SerialName("platform")
    val platform: Platform,
    @SerialName("redirect")
    val redirect: Int,
    @SerialName("series")
    val series: Boolean,
    @SerialName("seriesEntry")
    val seriesEntry: Int,
    @SerialName("images")
    val images: BgmSubjectImages?,
    @SerialName("rating")
    val rating: BgmSubjectRating?,
    @SerialName("summary")
    val summary: String,
    @SerialName("tags")
    val tags: List<Tag>,
    @SerialName("type")
    val type: Int,
    @SerialName("volumes")
    val volumes: Int
) {
    @Serializable
    data class Airtime(
        @SerialName("date")
        val date: String,
        @SerialName("month")
        val month: Int,
        @SerialName("weekday")
        val weekday: Int,
        @SerialName("year")
        val year: Int
    )

    @Serializable
    data class Collection(
        @SerialName("1")
        val x1: Int,
        @SerialName("2")
        val x2: Int,
        @SerialName("3")
        val x3: Int,
        @SerialName("4")
        val x4: Int,
        @SerialName("5")
        val x5: Int
    )

    @Serializable
    data class Platform(
        @SerialName("alias")
        val alias: String,
        @SerialName("enableHeader")
        val enableHeader: Boolean,
        @SerialName("id")
        val id: Int,
        @SerialName("order")
        val order: Int,
        @SerialName("type")
        val type: String,
        @SerialName("typeCN")
        val typeCN: String,
        @SerialName("wikiTpl")
        val wikiTpl: String
    )

    @Serializable
    data class Tag(
        @SerialName("count")
        val count: Int,
        @SerialName("name")
        val name: String
    )
}

@Serializable
data class BgmInfobox(
    val key: String,
    val values: List<BgmInfoboxValue>
)

@Serializable
data class BgmInfoboxValue(val v: String)


