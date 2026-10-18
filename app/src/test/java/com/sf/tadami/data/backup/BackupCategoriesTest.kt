package com.sf.tadami.data.backup

import com.sf.tadami.data.backup.models.Backup
import com.sf.tadami.data.backup.models.BackupAnime
import com.sf.tadami.data.backup.models.BackupCategory
import com.sf.tadami.data.backup.models.BackupPreference
import com.sf.tadami.data.backup.models.IntPreferenceValue
import com.sf.tadami.data.backup.models.StringPreferenceValue
import com.sf.tadami.data.backup.models.StringSetPreferenceValue
import com.sf.tadami.domain.category.Category
import com.sf.tadami.preferences.library.LibraryPreferences
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoNumber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalSerializationApi::class)
class BackupCategoriesTest {

    // The format of the backups made before the categories
    @Serializable
    private data class OldBackup(
        @ProtoNumber(1) val backupAnime: List<OldBackupAnime>,
        @ProtoNumber(100) val backupPreferences: List<BackupPreference> = emptyList(),
    )

    @Serializable
    private data class OldBackupAnime(
        @ProtoNumber(1) val source: Long,
        @ProtoNumber(2) val url: String,
        @ProtoNumber(3) val title: String = "",
        @ProtoNumber(16) val language: String? = null,
    )

    private fun category(id: Long, name: String, order: Long) = Category(id, name, order, 0L)

    @Test
    fun theCategoriesGoThroughABackup() {
        val backup = Backup(
            backupAnime = listOf(BackupAnime(source = 1L, url = "/a", title = "A", categories = listOf(3L, 7L))),
            backupCategories = listOf(BackupCategory("Action", order = 0, id = 3L), BackupCategory("Drama", order = 1, id = 7L, flags = 5L)),
        )

        val decoded = ProtoBuf.decodeFromByteArray(Backup.serializer(), ProtoBuf.encodeToByteArray(Backup.serializer(), backup))

        assertEquals(backup.backupCategories, decoded.backupCategories)
        assertEquals(listOf(3L, 7L), decoded.backupAnime.single().categories)
    }

    @Test
    fun aBackupFromBeforeTheCategoriesStillRestores() {
        val old = OldBackup(
            backupAnime = listOf(OldBackupAnime(source = 1L, url = "/a", title = "A", language = "VF")),
            backupPreferences = listOf(BackupPreference("library_sort_flags", StringPreferenceValue("x"))),
        )
        val bytes = ProtoBuf.encodeToByteArray(OldBackup.serializer(), old)

        val decoded = ProtoBuf.decodeFromByteArray(Backup.serializer(), bytes)

        assertEquals(emptyList<BackupCategory>(), decoded.backupCategories)
        val anime = decoded.backupAnime.single()
        assertEquals(listOf("/a", "A", "VF"), listOf(anime.url, anime.title, anime.language))
        assertEquals(emptyList<Long>(), anime.categories)
        assertEquals(old.backupPreferences, decoded.backupPreferences)
    }

    @Test
    fun aRestoreOnlyAddsTheCategoriesMissingByName() {
        val db = listOf(category(5L, "Action", 0L))
        val backup = listOf(
            BackupCategory("Later", order = 2, id = 9L),
            BackupCategory("Action", order = 0, id = 1L),
            BackupCategory("Drama", order = 1, id = 2L),
        )

        assertEquals(listOf("Drama", "Later"), categoriesToRestore(db, backup).map { it.name })
    }

    @Test
    fun theBackupIdsMapToTheLocalCategoriesByName() {
        val db = listOf(category(5L, "Action", 0L), category(6L, "Drama", 1L))
        val backup = listOf(BackupCategory("Action", id = 1L), BackupCategory("Drama", id = 2L), BackupCategory("Gone", id = 3L))

        assertEquals(mapOf(1L to 5L, 2L to 6L), backupCategoryIds(backup, db))
    }

    @Test
    fun theDefaultCategorySettingFollowsItsCategory() {
        val ids = mapOf(1L to 5L)
        fun default(id: Int) = BackupPreference(LibraryPreferences.DEFAULT_CATEGORY.name, IntPreferenceValue(id))

        assertEquals(default(5), default(1).withLocalCategoryIds(ids))
        // Ask and the Default category mean the same everywhere
        assertEquals(default(-1), default(-1).withLocalCategoryIds(emptyMap()))
        assertEquals(default(0), default(0).withLocalCategoryIds(emptyMap()))
        // An unknown category leaves the setting as it is
        assertNull(default(3).withLocalCategoryIds(ids))
    }

    @Test
    fun theUpdateCategoriesFollowTheirCategories() {
        val ids = mapOf(1L to 5L, 2L to 6L)
        fun include(vararg values: String) =
            BackupPreference(LibraryPreferences.UPDATE_CATEGORIES.name, StringSetPreferenceValue(values.toSet()))

        assertEquals(include("5", "0"), include("1", "0", "9").withLocalCategoryIds(ids))
        assertEquals(include(), include().withLocalCategoryIds(ids))
        // Only unknown categories: restoring an empty set would update them all
        assertNull(include("9").withLocalCategoryIds(ids))
    }

    @Test
    fun theOtherPreferencesAreLeftAsTheyAre() {
        val preference = BackupPreference("library_filter_flags", IntPreferenceValue(3))

        assertEquals(preference, preference.withLocalCategoryIds(emptyMap()))
    }
}
