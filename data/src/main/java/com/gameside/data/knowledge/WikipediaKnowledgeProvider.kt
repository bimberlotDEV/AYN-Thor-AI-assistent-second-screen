package com.gameside.data.knowledge

import com.gameside.domain.game.GameProfile
import com.gameside.domain.knowledge.GameKnowledgeProvider
import com.gameside.domain.knowledge.KnowledgeDocument
import com.gameside.domain.knowledge.KnowledgeSearchResult
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

@Singleton
class WikipediaKnowledgeProvider @Inject constructor() : GameKnowledgeProvider {
    override suspend fun search(game: GameProfile, query: String): List<KnowledgeSearchResult> = withContext(Dispatchers.IO) {
        val searchText = "${game.title} $query".take(MAX_QUERY_CHARS)
        searchOnce(searchText).ifEmpty { searchOnce(query.take(MAX_QUERY_CHARS)) }.ifEmpty { searchOnce(game.title) }
    }

    private fun searchOnce(searchText: String): List<KnowledgeSearchResult> {
        val url = "$API_ENDPOINT?action=query&list=search&srnamespace=0&srlimit=$SEARCH_LIMIT&format=json&formatversion=2&srsearch=${searchText.encoded()}"
        val array = request(url).getJSONObject("query").getJSONArray("search")
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val pageId = item.getLong("pageid").toString()
                val title = item.getString("title")
                add(
                    KnowledgeSearchResult(
                        id = pageId,
                        title = title,
                        sourceName = "Wikipedia",
                        url = "https://en.wikipedia.org/?curid=$pageId",
                        snippet = item.optString("snippet").replace(HTML_TAG, " ").replace(Regex("\\s+"), " ").trim(),
                    ),
                )
            }
        }
    }

    override suspend fun retrieve(result: KnowledgeSearchResult): KnowledgeDocument = withContext(Dispatchers.IO) {
        require(result.sourceName == "Wikipedia" && result.id.all(Char::isDigit)) { "Unsupported knowledge result." }
        val url = "$API_ENDPOINT?action=query&prop=extracts&explaintext=1&exchars=$EXTRACT_CHARS&format=json&formatversion=2&pageids=${result.id}"
        val pages = request(url).getJSONObject("query").getJSONArray("pages")
        val extract = if (pages.length() == 0) "" else pages.getJSONObject(0).optString("extract")
        KnowledgeDocument(result, extract, Instant.now())
    }

    private fun request(url: String): JSONObject {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = TIMEOUT_MILLIS
            connection.readTimeout = TIMEOUT_MILLIS
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "GameSideAI/0.1 (private Android companion)")
            val code = connection.responseCode
            if (code !in 200..299) throw IOException("Wikipedia returned HTTP $code")
            JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }

    private fun String.encoded(): String = URLEncoder.encode(this, StandardCharsets.UTF_8.name())

    private companion object {
        const val API_ENDPOINT = "https://en.wikipedia.org/w/api.php"
        const val SEARCH_LIMIT = 5
        const val EXTRACT_CHARS = 1_200
        const val MAX_QUERY_CHARS = 300
        const val TIMEOUT_MILLIS = 6_000
        val HTML_TAG = Regex("<[^>]+>")
    }
}
