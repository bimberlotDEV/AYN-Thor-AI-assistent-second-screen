package com.gameside.data.knowledge

import android.text.Html
import com.gameside.domain.game.GameProfile
import com.gameside.domain.knowledge.GameKnowledgeProvider
import com.gameside.domain.knowledge.KnowledgeDocument
import com.gameside.domain.knowledge.KnowledgeSearchResult
import java.io.IOException
import java.net.HttpURLConnection
import java.net.IDN
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

@Singleton
class MediaWikiGameKnowledgeProvider @Inject constructor() : GameKnowledgeProvider {
    private val autoSources = Collections.synchronizedMap(mutableMapOf<String, WikiSource?>())

    override suspend fun search(game: GameProfile, query: String): List<KnowledgeSearchResult> = withContext(Dispatchers.IO) {
        val source = resolveSource(game) ?: return@withContext emptyList()
        searchQueries(game, query).asSequence()
            .flatMap { searchOnce(game.id, source, it).asSequence() }
            .distinctBy(KnowledgeSearchResult::id)
            .take(SEARCH_LIMIT)
            .toList()
    }

    override suspend fun retrieve(result: KnowledgeSearchResult): KnowledgeDocument = withContext(Dispatchers.IO) {
        val endpoint = validatedEndpoint(result.sourceApiUrl)
        require(result.id.all(Char::isDigit)) { "Unsupported game-wiki result." }
        val url = "$endpoint?action=query&prop=extracts&explaintext=1&exchars=$EXTRACT_CHARS&format=json&formatversion=2&pageids=${result.id}"
        val pages = request(url).getJSONObject("query").getJSONArray("pages")
        val extract = if (pages.length() == 0) "" else pages.getJSONObject(0).optString("extract")
        val text = extract.ifBlank { retrieveRenderedPage(endpoint, result.id) }
        KnowledgeDocument(result, text, Instant.now())
    }

    private fun resolveSource(game: GameProfile): WikiSource? {
        game.preferredWikiSources.firstOrNull()?.let { configured ->
            val endpoint = validatedEndpoint(configured)
            return inspect(endpoint)
        }
        return autoSources.getOrPut(game.title.lowercase()) {
            candidateEndpoints(game.title).firstNotNullOfOrNull(::inspect)
        }
    }

    private fun candidateEndpoints(title: String): List<String> {
        val compact = title.lowercase().replace(Regex("[^a-z0-9]"), "")
        val dashed = title.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
        return buildList {
            if (compact.isNotEmpty()) {
                add("https://$compact.wiki.gg/api.php")
                add("https://$compact.fandom.com/api.php")
            }
            if (dashed.isNotEmpty() && dashed != compact) {
                add("https://$dashed.wiki.gg/api.php")
                add("https://$dashed.fandom.com/api.php")
            }
        }
    }

    private fun inspect(endpoint: String): WikiSource? = runCatching {
        val json = request("$endpoint?action=query&meta=siteinfo&siprop=general&format=json&formatversion=2")
        val general = json.getJSONObject("query").getJSONObject("general")
        WikiSource(endpoint, general.optString("sitename").ifBlank { URI(endpoint).host })
    }.getOrNull()

    private fun searchQueries(game: GameProfile, question: String): List<String> {
        val gameTerms = words(game.title).toSet()
        val entityTerms = words(question)
            .filterNot { it in STOP_WORDS || it in gameTerms }
            .distinct()
            .sortedByDescending(String::length)
        return (entityTerms.take(2) + question.trim().take(MAX_QUERY_CHARS))
            .filter(String::isNotBlank)
            .distinct()
    }

    private fun searchOnce(gameProfileId: String, source: WikiSource, searchText: String): List<KnowledgeSearchResult> {
        val url = "${source.endpoint}?action=query&list=search&srnamespace=0&srlimit=$PER_QUERY_LIMIT&format=json&formatversion=2&srsearch=${searchText.encoded()}"
        val array = request(url).getJSONObject("query").getJSONArray("search")
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val pageId = item.getLong("pageid").toString()
                val title = item.getString("title")
                add(
                    KnowledgeSearchResult(
                        id = pageId,
                        gameProfileId = gameProfileId,
                        title = title,
                        sourceName = source.name,
                        sourceApiUrl = source.endpoint,
                        url = pageUrl(source.endpoint, pageId),
                        snippet = item.optString("snippet").replace(HTML_TAG, " ").replace(WHITESPACE, " ").trim(),
                    ),
                )
            }
        }
    }

    private fun validatedEndpoint(input: String): String {
        val raw = input.trim()
        val initial = URI(raw)
        require(initial.scheme.equals("https", ignoreCase = true)) { "Game wiki must use HTTPS." }
        val asciiHost = IDN.toASCII(initial.host ?: error("Game wiki URL has no host."))
        require(asciiHost.contains('.') && !asciiHost.equals("localhost", true) && !IP_ADDRESS.matches(asciiHost)) {
            "Game wiki must use a public hostname."
        }
        val path = initial.path.orEmpty()
        val apiPath = when {
            path.endsWith("/api.php") -> path
            path.isBlank() || path == "/" -> "/api.php"
            else -> path.trimEnd('/') + "/api.php"
        }
        return URI("https", null, asciiHost, initial.port, apiPath, null, null).toString()
    }

    private fun pageUrl(endpoint: String, pageId: String): String {
        val uri = URI(endpoint)
        val indexPath = uri.path.replace(Regex("api\\.php$"), "index.php")
        return URI("https", null, uri.host, uri.port, indexPath, "curid=$pageId", null).toString()
    }

    private fun retrieveRenderedPage(endpoint: String, pageId: String): String {
        val url = "$endpoint?action=parse&pageid=$pageId&prop=text&disabletoc=1&disableeditsection=1&format=json&formatversion=2"
        val html = request(url).getJSONObject("parse").getString("text")
            .replace(NON_CONTENT_HTML, " ")
        return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString()
            .replace(WHITESPACE, " ")
            .trim()
            .take(MAX_RENDERED_TEXT_CHARS)
    }

    private fun request(url: String): JSONObject {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = TIMEOUT_MILLIS
            connection.readTimeout = TIMEOUT_MILLIS
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "GameSideAI/0.1 (private Android gaming companion)")
            val code = connection.responseCode
            if (code !in 200..299) throw IOException("Game wiki returned HTTP $code")
            val contentLength = connection.contentLengthLong
            if (contentLength > MAX_RESPONSE_CHARS) throw IOException("Game wiki response is too large.")
            val body = connection.inputStream.bufferedReader().use { reader ->
                val output = StringBuilder()
                val buffer = CharArray(8_192)
                while (true) {
                    val read = reader.read(buffer)
                    if (read < 0) break
                    output.append(buffer, 0, read)
                    if (output.length > MAX_RESPONSE_CHARS) throw IOException("Game wiki response is too large.")
                }
                output.toString()
            }
            JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun words(value: String): List<String> = WORD.findAll(value.lowercase()).map(MatchResult::value).filter { it.length >= 3 }.toList()
    private fun String.encoded(): String = URLEncoder.encode(this, StandardCharsets.UTF_8.name())

    private data class WikiSource(val endpoint: String, val name: String)

    private companion object {
        const val SEARCH_LIMIT = 6
        const val PER_QUERY_LIMIT = 4
        const val EXTRACT_CHARS = 1_200
        const val MAX_QUERY_CHARS = 250
        const val TIMEOUT_MILLIS = 6_000
        const val MAX_RESPONSE_CHARS = 2_000_000
        const val MAX_RENDERED_TEXT_CHARS = 12_000
        val HTML_TAG = Regex("<[^>]+>")
        val NON_CONTENT_HTML = Regex("""(?is)<(script|style|nav|table)\b.*?</\1>""")
        val WHITESPACE = Regex("\\s+")
        val WORD = Regex("[\\p{L}\\p{N}']+")
        val IP_ADDRESS = Regex("(?:\\d{1,3}\\.){3}\\d{1,3}|[0-9a-fA-F:]+")
        val STOP_WORDS = setOf(
            "about", "after", "against", "before", "does", "from", "give", "have", "help", "into", "location",
            "what", "when", "where", "which", "with", "without", "waar", "wanneer", "welke", "heeft", "voor",
            "naar", "over", "zonder", "locatie", "vind", "vinden", "krijg", "krijgen",
        )
    }
}
