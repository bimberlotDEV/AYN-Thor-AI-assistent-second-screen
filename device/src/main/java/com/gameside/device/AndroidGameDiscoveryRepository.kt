package com.gameside.device

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.provider.DocumentsContract
import com.gameside.domain.game.DiscoveredGame
import com.gameside.domain.game.GameDiscoveryRepository
import com.gameside.domain.game.GamePlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidGameDiscoveryRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : GameDiscoveryRepository {
    override suspend fun detectInstalledGamesAndEmulators(): List<DiscoveredGame> = withContext(Dispatchers.IO) {
        val manager = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        manager.queryIntentActivities(launcherIntent, 0)
            .asSequence()
            .filter { it.activityInfo.packageName != context.packageName }
            .distinctBy { it.activityInfo.packageName }
            .mapNotNull { resolved ->
                val info = resolved.activityInfo.applicationInfo
                val packageName = resolved.activityInfo.packageName
                val label = resolved.loadLabel(manager).toString().trim().ifBlank { packageName }
                val emulator = EmulatorCatalog.isEmulator(packageName, label)
                val game = info.category == ApplicationInfo.CATEGORY_GAME
                if (!emulator && !game) return@mapNotNull null
                DiscoveredGame(
                    stableKey = "package:$packageName",
                    title = label,
                    packageName = packageName,
                    platform = if (emulator) GamePlatform.EMULATED else GamePlatform.ANDROID,
                    sourceLabel = if (emulator) "Installed emulator" else "Installed Android game",
                )
            }
            .sortedWith(compareBy<DiscoveredGame> { it.platform != GamePlatform.ANDROID }.thenBy { it.title.lowercase() })
            .toList()
    }

    override suspend fun scanRomTree(treeUri: String): List<DiscoveredGame> = withContext(Dispatchers.IO) {
        val uri = runCatching { Uri.parse(treeUri) }.getOrNull() ?: return@withContext emptyList()
        val rootId = runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull() ?: return@withContext emptyList()
        val installedPackages = detectInstalledGamesAndEmulators().mapNotNull { it.packageName }.toSet()
        val found = mutableListOf<DiscoveredGame>()
        scanChildren(uri, rootId, "", installedPackages, found, depth = 0)
        found.distinctBy { it.stableKey }.sortedBy { it.title.lowercase() }
    }

    private fun scanChildren(
        treeUri: Uri,
        parentId: String,
        relativePath: String,
        installedPackages: Set<String>,
        output: MutableList<DiscoveredGame>,
        depth: Int,
    ) {
        if (depth > MAX_DEPTH || output.size >= MAX_ROMS) return
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
        )
        runCatching {
            context.contentResolver.query(children, projection, null, null, null)?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                while (cursor.moveToNext() && output.size < MAX_ROMS) {
                    val documentId = cursor.getString(idIndex)
                    val name = cursor.getString(nameIndex).orEmpty()
                    val mime = cursor.getString(mimeIndex).orEmpty()
                    val path = if (relativePath.isBlank()) name else "$relativePath/$name"
                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        scanChildren(treeUri, documentId, path, installedPackages, output, depth + 1)
                    } else {
                        RomCatalog.toDiscoveredGame(treeUri, documentId, path, name, installedPackages)?.let(output::add)
                    }
                }
            }
        }
    }

    private companion object {
        const val MAX_DEPTH = 10
        const val MAX_ROMS = 2_000
    }
}

internal object EmulatorCatalog {
    private val identifiers = listOf(
        "retroarch", "ppsspp", "dolphin", "citra", "lime3ds", "azahar", "yuzu", "sudachi",
        "aethersx2", "nethersx2", "duckstation", "drastic", "melonds", "mupen64", "redream",
        "flycast", "mame", "lemuroid", "vita3k", "emulator",
    )

    fun isEmulator(packageName: String, label: String): Boolean {
        val candidate = "$packageName $label".lowercase()
        return identifiers.any(candidate::contains)
    }

    fun packageFor(extension: String, path: String, installed: Set<String>): String? {
        val hints = when (extension) {
            "psp", "cso" -> listOf("ppsspp")
            "wbfs", "gcz", "rvz" -> listOf("dolphin")
            "3ds", "cia", "cci" -> listOf("azahar", "lime3ds", "citra")
            "nsp", "xci" -> listOf("sudachi", "yuzu")
            "nds" -> listOf("drastic", "melonds")
            "chd", "cue", "pbp" -> if (path.contains("ps2", true)) listOf("nethersx2", "aethersx2") else listOf("duckstation", "retroarch")
            "iso" -> when {
                path.contains("psp", true) -> listOf("ppsspp")
                path.contains("ps2", true) -> listOf("nethersx2", "aethersx2")
                else -> listOf("dolphin", "ppsspp", "nethersx2", "aethersx2")
            }
            "n64", "z64", "v64" -> listOf("mupen64", "retroarch")
            "gdi", "cdi" -> listOf("redream", "flycast")
            else -> listOf("retroarch", "lemuroid")
        }
        return hints.firstNotNullOfOrNull { hint -> installed.firstOrNull { it.contains(hint, ignoreCase = true) } }
    }
}

internal object RomCatalog {
    private val extensions = setOf(
        "iso", "cso", "chd", "cue", "pbp", "wbfs", "gcz", "rvz", "3ds", "cia", "cci",
        "nsp", "xci", "nds", "n64", "z64", "v64", "gdi", "cdi", "nes", "fds", "sfc",
        "smc", "gba", "gbc", "gb", "gen", "md", "sms", "gg", "pce", "a26", "a78",
        "zip", "7z",
    )

    fun toDiscoveredGame(
        treeUri: Uri,
        documentId: String,
        relativePath: String,
        fileName: String,
        installedPackages: Set<String>,
    ): DiscoveredGame? {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        if (extension !in extensions) return null
        val title = fileName.substringBeforeLast('.')
            .replace(Regex("[._]+"), " ")
            .replace(Regex("\\s*[\\[(].*?[\\])]"), "")
            .trim()
        if (title.isBlank()) return null
        val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
        return DiscoveredGame(
            stableKey = "rom:$documentUri",
            title = title,
            packageName = EmulatorCatalog.packageFor(extension, relativePath, installedPackages),
            platform = GamePlatform.EMULATED,
            sourceLabel = "ROM · .$extension${if (relativePath.contains('/')) " · ${relativePath.substringBeforeLast('/')}" else ""}",
        )
    }
}
