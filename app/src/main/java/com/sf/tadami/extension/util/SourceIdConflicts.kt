package com.sf.tadami.extension.util

import com.sf.tadami.domain.extensions.Extension
import com.sf.tadami.extension.model.ExtensionLoadIssue
import com.sf.tadami.extension.model.SourceIdConflict
import com.sf.tadami.source.Source

/**
 * The extensions whose sources can be registered, the others refused as
 * [ExtensionLoadIssue.SOURCE_ID_CONFLICT], and who owns each source ID after them. Rules, in order:
 * 1. an extension with two sources of the same ID is refused;
 * 2. an ID has one owner, the extension that has it, kept in [owners] while installed: another
 *    extension with a source of that ID is refused. On first sight, the extension signed with the
 *    official key gets the ID, then the one installed first.
 *
 * Applied again on each change of the extensions: a refused one keeps its sources aside, in
 * [Extension.Installed.refusedSources], and gets them back once its conflict is gone, its owner
 * uninstalled for instance.
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
): Pair<List<Extension.Installed>, Map<Long, String>> {
    val claims = extensions.mapNotNull { extension ->
        extension.claimedSources().takeIf { it.isNotEmpty() }?.let { sources ->
            SourceIdClaim(
                pkgName = extension.pkgName,
                ids = sources.map { it.id },
                official = isOfficial(extension),
                installedAt = installedAt(extension.pkgName),
            )
        }
    }
    val (conflicts, newOwners) = resolveClaims(claims, extensions.associate { it.pkgName to it.name }, owners)
    val resolved = extensions.map { extension ->
        val conflict = conflicts[extension.pkgName]
        when {
            conflict != null -> extension.copy(
                sources = emptyList(),
                refusedSources = extension.claimedSources(),
                loadIssue = ExtensionLoadIssue.SOURCE_ID_CONFLICT,
                sourceIdConflict = conflict,
            )
            extension.loadIssue == ExtensionLoadIssue.SOURCE_ID_CONFLICT -> extension.copy(
                sources = extension.refusedSources,
                refusedSources = emptyList(),
                loadIssue = null,
                sourceIdConflict = null,
            )
            else -> extension
        }
    }
    return resolved to newOwners
}

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
        .mapNotNull { extension ->
            extension.claimedSources().takeIf { it.isNotEmpty() }?.let { sources ->
                SourceIdClaim(extension.pkgName, sources.map { it.id }, isOfficial(extension), installedAt = 0L)
            }
        } + SourceIdClaim(pkgName, sourceIds, official, installedAt = Long.MAX_VALUE)
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
