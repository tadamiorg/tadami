package com.sf.tadami.desktop.extension

import com.sf.tadami.desktop.AppDirs
import com.sf.tadami.extension.model.LibVersion
import com.sf.tadami.network.GET
import com.sf.tadami.network.NetworkHelper
import com.sf.tadami.network.awaitSuccess
import com.sf.tadami.source.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.net.URLClassLoader

/** What an installed extension's files say of it, written at install. */
@Serializable
data class ExtensionMeta(
    val name: String,
    val pkgName: String,
    val versionName: String,
    val versionCode: Long,
    val sourceClasses: List<String>,
    val declaredSourceIds: List<Long>?,
    val signatureHash: String,
    val repoUrl: String,
)

/** An installed extension: its sources, or why it couldn't be loaded. */
data class InstalledExtension(
    val meta: ExtensionMeta,
    val sources: List<Source>,
    val icon: File?,
    val error: String? = null,
)

/**
 * Installs the extension APKs of a repo and loads their sources, the desktop's ExtensionsLoader:
 * the APK is checked (signature, lib version, manifest) before any of its code runs, then its dex is
 * converted to a jar loaded parent first, so the classes of the app win over any of the extension.
 */
class ExtensionManager(
    private val network: NetworkHelper,
    private val json: Json,
    private val repoUrl: String = DEFAULT_EXTENSIONS_REPO,
) {
    private val _available = MutableStateFlow<List<IndexEntry>>(emptyList())
    val available: StateFlow<List<IndexEntry>> = _available.asStateFlow()

    private val _installed = MutableStateFlow<List<InstalledExtension>>(emptyList())
    val installed: StateFlow<List<InstalledExtension>> = _installed.asStateFlow()

    suspend fun refreshAvailable() = withContext(Dispatchers.IO) {
        val body = network.client.newCall(GET("$repoUrl/index.min.json")).awaitSuccess().use { it.body.string() }
        _available.value = json.decodeFromString<List<IndexEntry>>(body)
    }

    /** Loads every installed extension, see [load]. */
    suspend fun loadInstalled() = withContext(Dispatchers.IO) {
        _installed.value = AppDirs.extensions.listFiles().orEmpty()
            .filter { File(it, META_FILE).isFile }
            .mapNotNull { dir -> runCatching { load(dir) }.getOrNull() }
            .sortedBy { it.meta.name }
    }

    /** Downloads, checks, converts and loads [entry], replacing its installed version. */
    suspend fun install(entry: IndexEntry): InstalledExtension = withContext(Dispatchers.IO) {
        val dir = File(AppDirs.extensions, entry.pkg)
        val staging = File(AppDirs.extensions, "${entry.pkg}.staging").apply { deleteRecursively(); mkdirs() }
        try {
            val apk = File(staging, APK_FILE)
            network.client.newCall(GET(entry.apkUrl(repoUrl))).awaitSuccess().use { response ->
                apk.outputStream().use { response.body.byteStream().copyTo(it) }
            }
            entry.size?.let { if (apk.length() != it) throw IOException("${entry.apk}: ${apk.length()} bytes, the index says $it") }
            entry.sha256?.let {
                val actual = ApkReader.sha256(apk.readBytes())
                if (!actual.equals(it, ignoreCase = true)) throw IOException("${entry.apk}: SHA-256 $actual, the index says $it")
            }
            val meta = inspect(apk)
            if (meta.pkgName != entry.pkg) throw IOException("${entry.apk} is ${meta.pkgName}, the index says ${entry.pkg}")
            DexConverter.convert(apk, File(staging, JAR_FILE))
            ApkReader.icon(apk)?.let { File(staging, ICON_FILE).writeBytes(it) }
            File(staging, META_FILE).writeText(json.encodeToString(ExtensionMeta.serializer(), meta))

            unloadFromState(entry.pkg)
            dir.deleteRecursively()
            if (!staging.renameTo(dir)) throw IOException("Can't move ${staging.path} to ${dir.path}")
            load(dir).also { loaded ->
                _installed.update { list -> (list.filter { it.meta.pkgName != entry.pkg } + loaded).sortedBy { it.meta.name } }
            }
        } finally {
            staging.deleteRecursively()
        }
    }

    fun uninstall(pkgName: String) {
        unloadFromState(pkgName)
        File(AppDirs.extensions, pkgName).deleteRecursively()
    }

    private fun unloadFromState(pkgName: String) {
        _installed.update { list ->
            list.filter { it.meta.pkgName != pkgName }
        }
    }

    /** What [apk] declares, refused when it isn't a usable extension signed with a trusted key. */
    private fun inspect(apk: File): ExtensionMeta {
        val signatures = ApkReader.signatureHashes(apk)
        if (signatures.none { it == OFFICIAL_SIGNATURE_HASH }) {
            throw IOException("Untrusted signature: ${signatures.lastOrNull() ?: "none or invalid"}")
        }
        val manifest = ApkReader.manifest(apk)
        if (EXTENSION_FEATURE !in manifest.features) throw IOException("${manifest.packageName} is not a Tadami extension")
        val libVersion = LibVersion.fromVersionName(manifest.versionName)
            ?: throw IOException("Invalid version ${manifest.versionName}")
        if (libVersion < API_VERSION_MIN || libVersion > API_VERSION_MAX) {
            throw IOException("Lib $libVersion, only $API_VERSION_MIN to $API_VERSION_MAX are supported")
        }
        val classes = manifest.metaData[METADATA_SOURCE_CLASS]
            ?.split(";")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.map { if (it.startsWith(".")) manifest.packageName + it else it }
            .orEmpty()
        if (classes.isEmpty()) throw IOException("Missing $METADATA_SOURCE_CLASS")
        val sourceIds = manifest.metaData[METADATA_SOURCE_IDS]?.takeIf { it.isNotBlank() }?.let { value ->
            value.split(';', ',').map { it.trim().toLongOrNull() ?: throw IOException("Malformed $METADATA_SOURCE_IDS") }
        }
        return ExtensionMeta(
            name = manifest.label.substringAfter("Tadami: "),
            pkgName = manifest.packageName,
            versionName = manifest.versionName,
            versionCode = manifest.versionCode,
            sourceClasses = classes,
            declaredSourceIds = sourceIds,
            signatureHash = signatures.last(),
            repoUrl = repoUrl,
        )
    }

    private fun load(dir: File): InstalledExtension {
        val meta = json.decodeFromString(ExtensionMeta.serializer(), File(dir, META_FILE).readText())
        val icon = File(dir, ICON_FILE).takeIf { it.isFile }
        return try {
            val loader = URLClassLoader(arrayOf(File(dir, JAR_FILE).toURI().toURL()), javaClass.classLoader)
            val sources = meta.sourceClasses.map { className ->
                when (val obj = Class.forName(className, false, loader).getDeclaredConstructor().newInstance()) {
                    is Source -> obj
                    else -> throw IOException("Unknown source class type: ${obj.javaClass}")
                }
            }
            val declared = meta.declaredSourceIds
            if (declared != null && declared.sorted() != sources.map { it.id }.sorted()) {
                throw IOException("Its manifest declares the source IDs $declared, its sources have ${sources.map { it.id }}")
            }
            InstalledExtension(meta, sources, icon)
        } catch (e: Throwable) {
            System.err.println("Extension ${meta.pkgName} failed to load: ${e.stackTraceToString()}")
            InstalledExtension(meta, emptyList(), icon, error = e.toString())
        }
    }

    companion object {
        private const val APK_FILE = "extension.apk"
        private const val JAR_FILE = "classes.jar"
        private const val ICON_FILE = "icon.png"
        private const val META_FILE = "extension.json"

        private const val EXTENSION_FEATURE = "tadami.extension"
        private const val METADATA_SOURCE_CLASS = "tadami.extension.class"
        private const val METADATA_SOURCE_IDS = "tadami.extension.sourceIds"

        val API_VERSION_MIN = LibVersion(1, 5)
        val API_VERSION_MAX = LibVersion(1, 7)

        /** SHA-256 fingerprint of the key the official extensions are signed with. */
        private const val OFFICIAL_SIGNATURE_HASH = "11dec208a209773efdc5348af5de709ca9af067e9154d1dd3c6abdb0fa3cc200"
    }
}
