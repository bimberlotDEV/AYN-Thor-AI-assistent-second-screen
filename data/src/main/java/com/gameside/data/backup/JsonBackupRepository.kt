package com.gameside.data.backup

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.gameside.data.database.BackupDao
import com.gameside.data.database.BackupData
import com.gameside.data.database.ChatMessageEntity
import com.gameside.data.database.ChatSessionEntity
import com.gameside.data.database.ChecklistItemEntity
import com.gameside.data.database.GameChecklistEntity
import com.gameside.data.database.GameNoteEntity
import com.gameside.data.database.GamePackageEntity
import com.gameside.data.database.GameProfileEntity
import com.gameside.data.database.GameWikiSourceEntity
import com.gameside.data.database.SavedAnswerEntity
import com.gameside.data.database.SourceCitationEntity
import com.gameside.data.database.QuickQuestionFavoriteEntity
import com.gameside.domain.backup.BackupRepository
import com.gameside.domain.backup.ImportResult
import dagger.hilt.android.qualifiers.ApplicationContext
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class JsonBackupRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dao: BackupDao,
) : BackupRepository {
    override suspend fun exportTo(destinationUri: String) = withContext(Dispatchers.IO) {
        val root = JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("exportedAtEpochMillis", System.currentTimeMillis())
            .put("containsCredentials", false)
            .put("games", dao.games().toArray(::gameJson))
            .put("packages", dao.packages().toArray { JSONObject().put("gameProfileId", it.gameProfileId).put("packageName", it.packageName) })
            .put("wikiSources", dao.wikiSources().toArray { JSONObject().put("gameProfileId", it.gameProfileId).put("url", it.url) })
            .put("sessions", dao.sessions().toArray(::sessionJson))
            .put("messages", dao.messages().toArray(::messageJson))
            .put("citations", dao.citations().toArray(::citationJson))
            .put("savedAnswers", dao.savedAnswers().toArray(::savedAnswerJson))
            .put("notes", dao.notes().toArray(::noteJson))
            .put("checklists", dao.checklists().toArray(::checklistJson))
            .put("checklistItems", dao.checklistItems().toArray(::checklistItemJson))
            .put("quickQuestionFavorites", dao.quickQuestionFavorites().toArray(::quickQuestionFavoriteJson))
        val uri = requireContentUri(destinationUri)
        context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter(StandardCharsets.UTF_8)?.use { it.write(root.toString(2)) }
            ?: error("Could not open the selected destination")
    }

    override suspend fun importFrom(sourceUri: String): ImportResult = withContext(Dispatchers.IO) {
        val uri = requireContentUri(sourceUri)
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader(StandardCharsets.UTF_8)?.use { reader ->
            val result = StringBuilder()
            val buffer = CharArray(8_192)
            while (true) {
                val size = reader.read(buffer)
                if (size < 0) break
                require(result.length + size <= MAX_FILE_CHARS) { "Backup is too large" }
                result.append(buffer, 0, size)
            }
            require(result.isNotEmpty()) { "Backup is empty" }
            result.toString()
        } ?: error("Could not open the selected backup")
        val root = JSONObject(text)
        require(root.optString("format") == FORMAT) { "This is not a GameSide AI backup" }
        require(root.optInt("version", -1) == VERSION) { "Unsupported backup version" }
        require(!root.optBoolean("containsCredentials", true)) { "Backups containing credentials are rejected" }
        val data = BackupData(
            games = root.safeArray("games").mapObjects(::parseGame),
            packages = root.safeArray("packages").mapObjects { GamePackageEntity(it.required("gameProfileId"), it.required("packageName")) },
            wikiSources = root.safeArray("wikiSources").mapObjects { GameWikiSourceEntity(it.required("gameProfileId"), it.requiredHttps("url")) },
            sessions = root.safeArray("sessions").mapObjects(::parseSession),
            messages = root.safeArray("messages").mapObjects(::parseMessage),
            citations = root.safeArray("citations").mapObjects(::parseCitation),
            savedAnswers = root.safeArray("savedAnswers").mapObjects(::parseSavedAnswer),
            notes = root.safeArray("notes").mapObjects(::parseNote),
            checklists = root.safeArray("checklists").mapObjects(::parseChecklist),
            checklistItems = root.safeArray("checklistItems").mapObjects(::parseChecklistItem),
            quickQuestionFavorites = root.optionalArray("quickQuestionFavorites").mapObjects(::parseQuickQuestionFavorite),
        )
        validateReferences(data)
        dao.importData(data)
        ImportResult(
            data.games.size,
            data.sessions.size,
            data.savedAnswers.size + data.notes.size + data.checklists.size + data.quickQuestionFavorites.size,
        )
    }

    private fun requireContentUri(value: String): Uri = Uri.parse(value).also {
        require(it.scheme == ContentResolver.SCHEME_CONTENT) { "Only a user-selected document is allowed" }
    }

    private fun validateReferences(data: BackupData) {
        val gameIds = data.games.mapTo(hashSetOf()) { it.id }
        val sessionIds = data.sessions.mapTo(hashSetOf()) { it.id }
        val messageIds = data.messages.mapTo(hashSetOf()) { it.id }
        val checklistIds = data.checklists.mapTo(hashSetOf()) { it.id }
        require(data.packages.all { it.gameProfileId in gameIds } && data.wikiSources.all { it.gameProfileId in gameIds }) { "Invalid game reference" }
        require(data.sessions.all { it.gameProfileId in gameIds }) { "Invalid conversation game" }
        require(data.messages.all { it.sessionId in sessionIds } && data.citations.all { it.messageId in messageIds }) { "Invalid conversation reference" }
        require(data.savedAnswers.all { it.gameProfileId in gameIds } && data.notes.all { it.gameProfileId in gameIds } && data.checklists.all { it.gameProfileId in gameIds }) { "Invalid personal-item game" }
        require(data.checklistItems.all { it.checklistId in checklistIds }) { "Invalid checklist reference" }
        require(data.quickQuestionFavorites.all { it.gameProfileId in gameIds }) { "Invalid quick-question game" }
    }

    private fun gameJson(v: GameProfileEntity) = JSONObject().put("id", v.id).put("title", v.title).put("platform", v.platform)
        .put("coverImageUri", v.coverImageUri).put("spoilerLevel", v.spoilerLevel).put("currentArea", v.currentArea)
        .put("currentChapter", v.currentChapter).put("currentQuest", v.currentQuest).put("customContext", v.customContext)
        .put("customSystemPrompt", v.customSystemPrompt).put("isPinned", v.isPinned).put("isArchived", v.isArchived)
        .put("createdAt", v.createdAtEpochMillis).put("updatedAt", v.updatedAtEpochMillis)

    private fun sessionJson(v: ChatSessionEntity) = JSONObject().put("id", v.id).put("gameProfileId", v.gameProfileId).put("title", v.title)
        .put("createdAt", v.createdAtEpochMillis).put("updatedAt", v.updatedAtEpochMillis)
    private fun messageJson(v: ChatMessageEntity) = JSONObject().put("id", v.id).put("sessionId", v.sessionId).put("role", v.role)
        .put("content", v.content).put("createdAt", v.createdAtEpochMillis)
    private fun citationJson(v: SourceCitationEntity) = JSONObject().put("messageId", v.messageId).put("position", v.position).put("title", v.title)
        .put("sourceName", v.sourceName).put("url", v.url).put("excerpt", v.excerpt).put("retrievedAt", v.retrievedAtEpochMillis)
    private fun savedAnswerJson(v: SavedAnswerEntity) = JSONObject().put("id", v.id).put("gameProfileId", v.gameProfileId)
        .put("sourceMessageId", v.sourceMessageId).put("question", v.question).put("answer", v.answer).put("citationsJson", v.citationsJson).put("createdAt", v.createdAtEpochMillis)
    private fun noteJson(v: GameNoteEntity) = JSONObject().put("id", v.id).put("gameProfileId", v.gameProfileId).put("title", v.title)
        .put("content", v.content).put("createdAt", v.createdAtEpochMillis).put("updatedAt", v.updatedAtEpochMillis)
    private fun checklistJson(v: GameChecklistEntity) = JSONObject().put("id", v.id).put("gameProfileId", v.gameProfileId).put("title", v.title)
        .put("createdAt", v.createdAtEpochMillis).put("updatedAt", v.updatedAtEpochMillis)
    private fun checklistItemJson(v: ChecklistItemEntity) = JSONObject().put("id", v.id).put("checklistId", v.checklistId).put("text", v.text)
        .put("isChecked", v.isChecked).put("position", v.position)
    private fun quickQuestionFavoriteJson(v: QuickQuestionFavoriteEntity) = JSONObject().put("id", v.id).put("gameProfileId", v.gameProfileId)
        .put("label", v.label).put("question", v.question).put("category", v.category).put("position", v.position).put("createdAt", v.createdAtEpochMillis)

    private fun parseGame(o: JSONObject) = GameProfileEntity(
        o.required("id"), o.required("title"), o.requiredEnum("platform", PLATFORMS), o.nullable("coverImageUri"),
        o.requiredEnum("spoilerLevel", SPOILER_LEVELS), o.nullable("currentArea"), o.nullable("currentChapter"),
        o.nullable("currentQuest"), o.nullable("customContext"), o.nullable("customSystemPrompt"), o.getBoolean("isPinned"),
        o.getBoolean("isArchived"), o.getLong("createdAt"), o.getLong("updatedAt"),
    )
    private fun parseSession(o: JSONObject) = ChatSessionEntity(o.required("id"), o.required("gameProfileId"), o.required("title"), o.getLong("createdAt"), o.getLong("updatedAt"))
    private fun parseMessage(o: JSONObject) = ChatMessageEntity(o.required("id"), o.required("sessionId"), o.requiredEnum("role", ROLES), o.required("content", MAX_CONTENT), o.getLong("createdAt"))
    private fun parseCitation(o: JSONObject) = SourceCitationEntity(o.required("messageId"), o.getInt("position"), o.required("title"), o.required("sourceName"), o.requiredHttps("url"), o.required("excerpt", MAX_CONTENT), o.getLong("retrievedAt"))
    private fun parseSavedAnswer(o: JSONObject) = SavedAnswerEntity(o.required("id"), o.required("gameProfileId"), o.required("sourceMessageId"), o.required("question", MAX_CONTENT), o.required("answer", MAX_CONTENT), o.required("citationsJson", MAX_CONTENT), o.getLong("createdAt"))
    private fun parseNote(o: JSONObject) = GameNoteEntity(o.required("id"), o.required("gameProfileId"), o.required("title"), o.required("content", MAX_CONTENT), o.getLong("createdAt"), o.getLong("updatedAt"))
    private fun parseChecklist(o: JSONObject) = GameChecklistEntity(o.required("id"), o.required("gameProfileId"), o.required("title"), o.getLong("createdAt"), o.getLong("updatedAt"))
    private fun parseChecklistItem(o: JSONObject) = ChecklistItemEntity(o.required("id"), o.required("checklistId"), o.required("text"), o.getBoolean("isChecked"), o.getInt("position"))
    private fun parseQuickQuestionFavorite(o: JSONObject) = QuickQuestionFavoriteEntity(
        o.required("id"), o.required("gameProfileId"), o.required("label"), o.required("question", MAX_CONTENT),
        o.requiredEnum("category", QUESTION_CATEGORIES), o.getInt("position"), o.getLong("createdAt"),
    )

    private fun JSONObject.required(name: String, max: Int = MAX_SHORT): String = getString(name).also { require(it.isNotBlank() && it.length <= max) { "Invalid $name" } }
    private fun JSONObject.requiredHttps(name: String): String = required(name, 2_048).also { require(Uri.parse(it).scheme == "https") { "Invalid $name" } }
    private fun JSONObject.requiredEnum(name: String, allowed: Set<String>): String = required(name).also { require(it in allowed) { "Invalid $name" } }
    private fun JSONObject.nullable(name: String): String? = if (isNull(name)) null else required(name, MAX_CONTENT)
    private fun JSONObject.safeArray(name: String): JSONArray = getJSONArray(name).also { require(it.length() <= MAX_RECORDS) { "Too many $name records" } }
    private fun JSONObject.optionalArray(name: String): JSONArray = optJSONArray(name)?.also { require(it.length() <= MAX_RECORDS) { "Too many $name records" } } ?: JSONArray()
    private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> = List(length()) { transform(getJSONObject(it)) }
    private fun <T> List<T>.toArray(transform: (T) -> JSONObject) = JSONArray().also { array -> forEach { array.put(transform(it)) } }

    private companion object {
        const val FORMAT = "gameside-ai-backup"
        const val VERSION = 1
        const val MAX_FILE_CHARS = 10_000_000
        const val MAX_RECORDS = 20_000
        const val MAX_SHORT = 512
        const val MAX_CONTENT = 100_000
        val PLATFORMS = setOf("ANDROID", "EMULATED", "PC_STREAMING", "CONSOLE", "OTHER")
        val SPOILER_LEVELS = setOf("NONE", "MINIMAL", "MODERATE", "FULL")
        val ROLES = setOf("USER", "ASSISTANT")
        val QUESTION_CATEGORIES = setOf("NAVIGATION", "BOSS", "ITEM", "QUEST", "PUZZLE", "SETTINGS")
    }
}
