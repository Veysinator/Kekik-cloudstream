// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import android.util.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.fasterxml.jackson.annotation.JsonProperty

open class Sobreatsesuyp : ExtractorApi() {
    override val name            = "Sobreatsesuyp"
    override val mainUrl         = "https://sobreatsesuyp.com"
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        val extRef = referer ?: ""

        val videoReq = app.get(url, referer = extRef).text

        val file     = Regex("""file\":\"([^\"]+)""").find(videoReq)?.groupValues?.get(1) ?: throw ErrorLoadingException("File not found")
        val postLink = "${mainUrl}/" + file.replace("\\", "")
        val rawList  = app.post(postLink, referer = extRef).parsedSafe<List<Any>>() ?: throw ErrorLoadingException("Post link not found")

        val postJson: List<SobreatsesuypVideoData> = rawList.drop(1).map { item ->
            val mapItem = item as Map<*, *>
            SobreatsesuypVideoData(
                title = mapItem["title"] as? String,
                file  = mapItem["file"]  as? String
            )
        }
        Log.d("Kekik_${this.name}", "postJson » ${postJson}")

		val vidLinks = mutableSetOf<String>()
        val vidPairs = mutableListOf<Pair<String, String>>()
        for (item in postJson) {
            if (item.file == null || item.title == null) continue

            val fileUrl   = "${mainUrl}/playlist/${item.file.substring(1)}.txt"
            val videoData = app.post(fileUrl, referer = extRef).text

			if (videoData in vidLinks) { continue }
 			vidLinks.add(videoData)

            vidPairs.add(Pair(item.title, videoData))
        }

        for (vidPair in vidPairs) {
            Log.d("Kekik_${this.name}", "vidPair » ${vidPair}")
            val (title, m3uLink) = vidPair

	        callback.invoke(
                ExtractorLink(
                    source  = this.name,
                    name    = "${this.name} - ${title}",
                    url     = m3uLink,
                    referer = extRef,
                    quality = Qualities.Unknown.value,
                    type    = INFER_TYPE
                )
            )
        }
    }

    data class SobreatsesuypVideoData(
        @JsonProperty("title") val title: String? = null,
        @JsonProperty("file")  val file: String?  = null
    )
}