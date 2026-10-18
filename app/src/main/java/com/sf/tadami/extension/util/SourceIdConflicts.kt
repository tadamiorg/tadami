package com.sf.tadami.extension.util

import com.sf.tadami.domain.extensions.Extension
import com.sf.tadami.extension.model.ExtensionLoadIssue
import com.sf.tadami.extension.model.SourceIdConflict
import com.sf.tadami.source.Source

/**
 * The IDs of the official sources, set by hand before the IDs were generated from the repo, the
 * package and the source class: only an extension signed with the official key may use them.
 * gogoanime 1, animesama 2, vostfree 3, voiranime 4, otakufr 5, anisama 6, animepahe 7, hianime 8,
 * jetanime 11, franime 15, kickassanime 101, ianimes 102.
 */
internal val OFFICIAL_SOURCE_IDS = setOf(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 11L, 15L, 101L, 102L)

/**
 * The extensions whose sources can be registered, the others refused as
 * [ExtensionLoadIssue.SOURCE_ID_CONFLICT], and who owns each source ID after them. Rules, in order:
 * 1. an extension with two sources of the same ID is refused;
 * 2. an extension not signed with the official key using one of the [OFFICIAL_SOURCE_IDS] is refused;
 * 3. an ID has one owner, the extension that has it, kept in [owners] while installed: another
 *    extension with a source of that ID is refused. On first sight, the extension signed with the
 *    official key gets the ID, then the one installed first.
 *
 * Applied again on each change of the extensions: a refused one keeps its sources aside, in
 * [Extension.Installed.refusedSources], and gets them back once its conflict is gone, its owner
 * uninstalled for instance. One refused before its code was loaded, see [conflictsBeforeLoading],
 * claims its [Extension.Installed.declaredSourceIds] instead, and is to be loaded once its conflict
 * is gone, see [SourceIdResolution.toLoad].
 *
 * @param extensions every installed extension, usable or not
 * @param owners the owner, a package name, of each source ID
 * @param isOfficial whether an extension is signed with the official key
 * @param installedAt when the extension of a package name was installed, to order them on first sight
 * @return [extensions] with the conflicts set or cleared, and the owners to keep
 */
internal fun resolveSourceIdConflicts(
    extensions: List<Extension.Installed>,
    owners: Map<Long, String>,
    isOfficial: (Extension.Installed) -> Boolean,
    installedAt: (String) -> Long,
): SourceIdResolution {
    val claims = extensions.mapNotNull { it.sourceIdClaim(isOfficial, installedAt) }
    val (conflicts, newOwners) = resolveClaims(claims, extensions.associate { it.pkgName to it.name }, owners)
    val toLoad = mutableListOf<String>()
    val resolved = extensions.map { extension ->
        val conflict = conflicts[extension.pkgName]
        when {
            conflict != null -> extension.copy(
                sources = emptyList(),
                refusedSources = extension.claimedSources(),
                loadIssue = ExtensionLoadIssue.SOURCE_ID_CONFLICT,
                sourceIdConflict = conflict,
            )
            extension.loadIssue != ExtensionLoadIssue.SOURCE_ID_CONFLICT -> extension
            // Refused before its code was loaded: without sources until it's loaded, asked once
            extension.refusedSources.isEmpty() -> {
                if (extension.sourceIdConflict != null) toLoad += extension.pkgName
                extension.copy(sourceIdConflict = null)
            }
            else -> extension.copy(
                sources = extension.refusedSources,
                refusedSources = emptyList(),
                loadIssue = null,
                sourceIdConflict = null,
            )
        }
    }
    return SourceIdResolution(resolved, newOwners, toLoad)
}

/**
 * What [resolveSourceIdConflicts] gives.
 *
 * @property extensions the extensions with their conflicts set or cleared
 * @property owners the owners to keep
 * @property toLoad the extensions refused before their code was loaded whose conflict is gone, to
 * load now: they're listed without sources until then
 */
internal data class SourceIdResolution(
    val extensions: List<Extension.Installed>,
    val owners: Map<Long, String>,
    val toLoad: List<String>,
)

/**
 * Why each extension about to be loaded, of [claims], would be refused, by package name, from the
 * source IDs its manifest declares, so that its code is never loaded then: the rules of
 * [resolveSourceIdConflicts], applied to these claims and to those of the [installed] extensions not
 * being loaded. An extension that doesn't declare its IDs is only checked once loaded.
 *
 * @param claims the declared IDs of the extensions about to be loaded that would run their code
 * @param loading the name of each extension about to be loaded, by package name, those of [claims]
 * included: an owner among them, even one declaring no ID, keeps its IDs
 * @param installed the extensions registered
 */
internal fun conflictsBeforeLoading(
    claims: List<SourceIdClaim>,
    loading: Map<String, String>,
    installed: List<Extension.Installed>,
    owners: Map<Long, String>,
    isOfficial: (Extension.Installed) -> Boolean,
    installedAt: (String) -> Long,
): Map<String, SourceIdConflict> {
    val declaring = claims.map { it.pkgName }.toSet()
    val others = installed.filter { it.pkgName !in loading && it.pkgName !in declaring }
    val allClaims = others.mapNotNull { it.sourceIdClaim(isOfficial, installedAt) } + claims
    val names = others.associate { it.pkgName to it.name } + loading
    return resolveClaims(allClaims, names, owners).first.filterKeys { it in declaring }
}

/**
 * The source IDs of the meta-data value [value], decimal IDs separated by `;` or `,`, see
 * [Extension.Installed.declaredSourceIds]: null when there's none, empty when one isn't an ID.
 */
internal fun parseDeclaredSourceIds(value: String?): List<Long>? {
    if (value.isNullOrBlank()) return null
    val ids = value.split(';', ',').map { it.trim().toLongOrNull() }
    return if (ids.any { it == null }) emptyList() else ids.filterNotNull()
}

/**
 * Whether the sources of IDs [loaded] aren't those the manifest declares, [declared]: a lying
 * manifest, which may have passed the check made before its code was loaded.
 */
internal fun sourceIdsMismatch(declared: List<Long>?, loaded: List<Long>): Boolean =
    declared != null && declared.sorted() != loaded.sorted()

/**
 * Why the extension [pkgName] of the sources [sourceIds] would be refused once installed, in place
 * of the installed one of that package if any, see [resolveSourceIdConflicts]. Null when it wouldn't.
 */
internal fun sourceIdConflictOf(
    pkgName: String,
    sourceIds: List<Long>,
    official: Boolean,
    extensions: List<Extension.Installed>,
    owners: Map<Long, String>,
    isOfficial: (Extension.Installed) -> Boolean,
): SourceIdConflict? {
    val claims = extensions
        .filter { it.pkgName != pkgName }
        .mapNotNull { it.sourceIdClaim(isOfficial) { 0L } } +
        SourceIdClaim(pkgName, sourceIds, official, installedAt = Long.MAX_VALUE)
    val names = extensions.associate { it.pkgName to it.name }
    return resolveClaims(claims, names + (pkgName to (names[pkgName] ?: pkgName)), owners).first[pkgName]
}

/** The source IDs [ids] the extension [pkgName] would register. */
internal data class SourceIdClaim(
    val pkgName: String,
    val ids: List<Long>,
    val official: Boolean,
    val installedAt: Long,
)

/**
 * The conflict of each of [claims] refused, by package name, and the owners after them, see
 * [resolveSourceIdConflicts].
 *
 * @param installed the name of each installed extension, by package name: an owner not installed
 * anymore owns nothing
 */
internal fun resolveClaims(
    claims: List<SourceIdClaim>,
    installed: Map<String, String>,
    owners: Map<Long, String>,
): Pair<Map<String, SourceIdConflict>, Map<Long, String>> {
    val conflicts = mutableMapOf<String, SourceIdConflict>()
    val candidates = claims.filter { claim ->
        val conflict = when {
            claim.ids.size != claim.ids.toSet().size -> SourceIdConflict.Duplicate
            !claim.official && claim.ids.any { it in OFFICIAL_SOURCE_IDS } -> SourceIdConflict.Reserved
            else -> null
        }
        conflict?.let { conflicts[claim.pkgName] = it }
        conflict == null
    }

    val taken = owners.filterValues { it in installed }.toMutableMap()
    val firstSightOrder = compareByDescending<SourceIdClaim> { it.official }
        .thenBy { it.installedAt }
        .thenBy { it.pkgName }
    for (claim in candidates.sortedWith(firstSightOrder)) {
        val owner = claim.ids.firstNotNullOfOrNull { id -> taken[id]?.takeIf { it != claim.pkgName } }
        if (owner != null) {
            conflicts[claim.pkgName] = SourceIdConflict.TakenBy(owner, installed[owner] ?: owner)
        } else {
            claim.ids.forEach { taken[it] = claim.pkgName }
        }
    }
    return conflicts to taken
}

/** The sources this extension would register: its own, or those a conflict keeps aside. */
private fun Extension.Installed.claimedSources(): List<Source> = when (loadIssue) {
    null -> sources
    ExtensionLoadIssue.SOURCE_ID_CONFLICT -> refusedSources
    else -> emptyList()
}

/**
 * The claim of the source IDs this extension would register, see [claimedSources], or of its
 * declared ones when it was refused before its code was loaded. Null when it has none.
 */
private fun Extension.Installed.sourceIdClaim(
    isOfficial: (Extension.Installed) -> Boolean,
    installedAt: (String) -> Long,
): SourceIdClaim? {
    val ids = claimedSources().map { it.id }.ifEmpty {
        declaredSourceIds.takeIf { loadIssue == ExtensionLoadIssue.SOURCE_ID_CONFLICT }.orEmpty()
    }
    if (ids.isEmpty()) return null
    return SourceIdClaim(pkgName, ids, isOfficial(this), installedAt(pkgName))
}
