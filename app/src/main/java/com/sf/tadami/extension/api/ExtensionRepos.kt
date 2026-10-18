package com.sf.tadami.extension.api

import com.sf.tadami.preferences.sources.SourcesPreferences
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.text.Normalizer

/** The URLs of the extension repos: a repo is the base URL its `index.min.json` is under. */
internal object ExtensionRepos {

    private const val INDEX_FILE = "index.min.json"
    private const val META_FILE = "repo.json"

    /** The branches a GitHub repo URL is tried with, in this order: `repo` is keiyoushi's. */
    private val GITHUB_BRANCHES = listOf("main", "master", "repo")

    /** What the user typed to add a repo. */
    sealed interface Input {
        /** The base URLs to try, in this order: the first one whose index can be read is the repo. */
        data class Candidates(val urls: List<String>) : Input

        /** An http URL, refused but in debug builds. */
        data object NotHttps : Input

        data object Invalid : Input
    }

    /** [url] without spaces around it, its index file nor trailing slashes. */
    fun normalize(url: String): String =
        url.trim().trimEnd('/').removeSuffix("/$INDEX_FILE").trimEnd('/')

    /** The base URLs [input] may be, see [Input]. */
    fun parse(input: String, allowHttp: Boolean): Input {
        val url = normalize(input)
        val scheme = url.substringBefore("://", missingDelimiterValue = "").lowercase()
        val rest = url.substringAfter("://")
        val host = rest.substringBefore('/')
        if (host.isEmpty() || host.any { it.isWhitespace() } || rest.any { it.isWhitespace() }) return Input.Invalid
        when (scheme) {
            "https" -> {}
            "http" -> if (!allowHttp) return Input.NotHttps
            else -> return Input.Invalid
        }
        val path = rest.substringAfter('/', missingDelimiterValue = "").split('/')
        val isGitHubRepo = host.lowercase().removePrefix("www.") == "github.com" && path.size == 2 && path.none { it.isEmpty() }
        if (isGitHubRepo) {
            val (owner, repo) = path
            return Input.Candidates(GITHUB_BRANCHES.map { "https://raw.githubusercontent.com/$owner/$repo/$it" })
        }
        return Input.Candidates(listOf(url))
    }

    /** Where the index of the repo [repoUrl] is. */
    fun indexUrl(repoUrl: String): String = "$repoUrl/$INDEX_FILE"

    /** Where the description of the repo [repoUrl] is, if it has one, see [parseMeta]. */
    fun metaUrl(repoUrl: String): String = "$repoUrl/$META_FILE"

    /** The description in [text], the content of a `repo.json`, as Mihon's: null when it has none. */
    fun parseMeta(json: Json, text: String): RepoMeta? {
        val meta = runCatching { json.decodeFromString(RepoJson.serializer(), text).meta }.getOrNull() ?: return null
        if (meta.name.isBlank()) return null
        return meta.copy(signingKeyFingerprint = meta.signingKeyFingerprint?.let(::normalizeFingerprint))
    }

    /** [fingerprint], a hex SHA-256, lowercase without separators as the app stores it, null if it isn't one. */
    private fun normalizeFingerprint(fingerprint: String): String? =
        fingerprint.filterNot { it == ':' || it.isWhitespace() }.lowercase()
            .takeIf { hex -> hex.length == 64 && hex.all { it in '0'..'9' || it in 'a'..'f' } }

    private const val MAX_NAME_LENGTH = 40

    /** What a repo can't call itself: it would pass for the official repo, or the app's. */
    private val RESERVED_NAMES = listOf("official", "officiel", "tadami")

    /** Letters of other scripts, digits and signs that read as latin letters, to the letter. */
    private val HOMOGLYPHS = mapOf(
        // Cyrillic
        '\u0430' to 'a', '\u0432' to 'b', '\u0435' to 'e', '\u0451' to 'e', '\u043a' to 'k', '\u043c' to 'm',
        '\u043d' to 'h', '\u043e' to 'o', '\u0440' to 'p', '\u0441' to 'c', '\u0442' to 't', '\u0443' to 'y',
        '\u0445' to 'x', '\u0456' to 'i', '\u0457' to 'i', '\u0458' to 'j', '\u0455' to 's', '\u0501' to 'd',
        '\u051b' to 'q', '\u051d' to 'w', '\u04cf' to 'l',
        // Greek
        '\u03b1' to 'a', '\u03b2' to 'b', '\u03b5' to 'e', '\u03b7' to 'n', '\u03b9' to 'i', '\u03ba' to 'k',
        '\u03bc' to 'm', '\u03bd' to 'v', '\u03bf' to 'o', '\u03c1' to 'p', '\u03c4' to 't', '\u03c5' to 'u',
        '\u03c7' to 'x', '\u03f2' to 'c', '\u0131' to 'i',
        // Digits and signs
        '0' to 'o', '1' to 'l', '3' to 'e', '4' to 'a', '5' to 's', '7' to 't', '@' to 'a', '$' to 's', '|' to 'l',
        '!' to 'l',
        // I and l look alike
        'i' to 'l',
    )

    /**
     * [name], the name a repo gives itself in its `repo.json`, as it can be shown: without control
     * nor invisible characters, [MAX_NAME_LENGTH] characters at most, null when it's empty or could
     * pass for the official repo or the app, however it's written, see [RESERVED_NAMES]. Only the
     * app calls a repo official, see [isOfficial].
     */
    fun safeName(name: String?): String? {
        val cleaned = name.orEmpty()
            .filterNot { Character.isISOControl(it) || Character.getType(it) == Character.FORMAT.toInt() }
            .trim()
        if (cleaned.isEmpty()) return null
        val skeleton = skeleton(cleaned)
        if (RESERVED_NAMES.any { skeleton(it) in skeleton }) return null
        return if (cleaned.length > MAX_NAME_LENGTH) cleaned.take(MAX_NAME_LENGTH).trimEnd() + "\u2026" else cleaned
    }

    /** [text] as the latin letters it reads as, lowercase, without anything else, see [HOMOGLYPHS]. */
    private fun skeleton(text: String): String = buildString {
        // Compatibility forms to their letters: full width, ligatures, styled ones... then accents apart
        for (c in Normalizer.normalize(text, Normalizer.Form.NFKD)) {
            if (Character.getType(c) == Character.NON_SPACING_MARK.toInt()) continue
            val lower = c.lowercaseChar()
            val letter = HOMOGLYPHS[lower] ?: lower
            if (letter in 'a'..'z') append(letter)
        }
    }

    /** A readable name of the repo [repoUrl]: `owner/repo` on GitHub, its host and path otherwise. */
    fun name(repoUrl: String): String {
        val repo = normalize(repoUrl).substringAfter("://")
        val parts = repo.split('/')
        val host = parts.first().lowercase().removePrefix("www.")
        val onGitHub = host == "raw.githubusercontent.com" || host == "github.com"
        return if (onGitHub && parts.size >= 3) "${parts[1]}/${parts[2]}" else repo
    }

    /**
     * Whether [repoUrl] is the official repo: only the app's own URL of it, scheme included, see
     * [SourcesPreferences.DEFAULT_EXTENSIONS_REPO], so that no other repo can be shown as the official one.
     */
    fun isOfficial(repoUrl: String): Boolean =
        normalize(repoUrl) == normalize(SourcesPreferences.DEFAULT_EXTENSIONS_REPO)
}

/** What a repo says of itself in its `repo.json`. */
@Serializable
data class RepoMeta(
    val name: String,
    val shortName: String? = null,
    val website: String? = null,
    /** The SHA-256 fingerprint of the key its extensions are signed with. */
    val signingKeyFingerprint: String? = null,
)

@Serializable
private data class RepoJson(val meta: RepoMeta)
