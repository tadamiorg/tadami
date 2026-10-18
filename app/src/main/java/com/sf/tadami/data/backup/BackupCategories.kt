package com.sf.tadami.data.backup

import com.sf.tadami.data.backup.models.BackupCategory
import com.sf.tadami.data.backup.models.BackupPreference
import com.sf.tadami.data.backup.models.IntPreferenceValue
import com.sf.tadami.data.backup.models.StringSetPreferenceValue
import com.sf.tadami.domain.category.Category
import com.sf.tadami.preferences.library.LibraryPreferences

// A restore merges the categories by name: the ones of the backup missing here are added, the others are
// the same categories. Ids differ between installs, so the backup's ones are mapped to the local ones

/** The backup's categories missing from [dbCategories], in the backup's order */
internal fun categoriesToRestore(
    dbCategories: List<Category>,
    backupCategories: List<BackupCategory>
): List<BackupCategory> {
    val dbNames = dbCategories.mapTo(HashSet()) { it.name }
    return backupCategories
        .filter { it.name !in dbNames }
        .sortedBy { it.order }
        .distinctBy { it.name }
}

/** The local id of each backup category id, matched by name */
internal fun backupCategoryIds(
    backupCategories: List<BackupCategory>,
    dbCategories: List<Category>
): Map<Long, Long> {
    val dbIdsByName = dbCategories.associate { it.name to it.id }
    return backupCategories
        .mapNotNull { category -> dbIdsByName[category.name]?.let { category.id to it } }
        .toMap()
}

/**
 * The category settings with the local ids, the other preferences as they are. Null to leave a setting as
 * it is: none of its categories is known here, the backup has no categories or they aren't restored.
 */
internal fun BackupPreference.withLocalCategoryIds(categoryIds: Map<Long, Long>): BackupPreference? {
    // Ids that mean the same everywhere: ask, and the Default category
    val sharedIds = setOf(LibraryPreferences.DEFAULT_CATEGORY_ASK.toLong(), Category.UNCATEGORIZED_ID)
    val localId: (Long) -> Long? = { id -> if (id in sharedIds) id else categoryIds[id] }
    return when {
        key == LibraryPreferences.DEFAULT_CATEGORY.name && value is IntPreferenceValue -> {
            localId(value.value.toLong())?.let { copy(value = IntPreferenceValue(it.toInt())) }
        }
        key in categorySetKeys && value is StringSetPreferenceValue -> {
            val ids = value.value.mapNotNull { it.toLongOrNull()?.let(localId)?.toString() }.toSet()
            // Empty means all the categories to include: not the same as some unknown ones
            if (value.value.isNotEmpty() && ids.isEmpty()) null else copy(value = StringSetPreferenceValue(ids))
        }
        else -> this
    }
}

private val categorySetKeys = setOf(
    LibraryPreferences.UPDATE_CATEGORIES.name,
    LibraryPreferences.UPDATE_CATEGORIES_EXCLUDE.name
)
