package com.sf.tadami.preferences.library

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.sf.tadami.preferences.model.CustomPreferences
import com.sf.tadami.preferences.model.CustomPreferencesIdentifier

data class LibraryPreferences(
    val portraitColumns: Int,
    val landscapeColumns: Int,
    val autoUpdateInterval: Int,
    val autoUpdateRestrictions: Set<String>,
    val filterFlags: LibraryFilter,
    val sortFlags : LibrarySort,
    val lastUpdatedTimestamp : Long,
    val newUpdatesCount : Int,
    // A category id, DEFAULT_CATEGORY_ASK to ask with the set categories dialog
    val defaultCategory : Int,
    // Category ids as strings, the Default category's included. No category to include updates them all
    val updateCategories : Set<String>,
    val updateCategoriesExclude : Set<String>,
    // The library updates fetch the details and cover of each anime too, as Mihon's
    val autoUpdateMetadata : Boolean,
    // A details refresh replaces the titles of library anime with their source's, as Mihon's updateMangaTitles
    val updateTitles : Boolean,
    // The item count on each category tab
    val showCategoryNumberOfItems : Boolean,
    // The category tab row, the title names the category shown without it
    val showCategoryTabs : Boolean,
    // Each category has its own sort, kept in its flags, instead of the library one
    val categorizedSort : Boolean,
    // The Default category's sort with the per category sort, as it isn't stored. 0 until one is chosen
    val defaultCategorySortFlags : Long
) : CustomPreferencesIdentifier {

    object AutoUpdateRestrictionItems {
        const val WIFI = "wifi"
        const val CHARGE = "charge"
        const val BATTERY = "battery"
    }

    object AutoUpdateIntervalItems {
        const val DISABLED = 0
        const val TWELVE = 12
        const val DAILY = 24
        const val DAILY_2 = 48
        const val DAILY_3 = 72
        const val WEEKLY = 168
    }

    companion object : CustomPreferences<LibraryPreferences> {
        private val PORTRAIT_COLUMNS = intPreferencesKey("library_portrait_columns")
        private val LANDSCAPE_COLUMNS = intPreferencesKey("library_landscape_columns")
        private val AUTO_UPDATE_INTERVAL = intPreferencesKey("library_auto_update_interval")
        private val AUTO_UPDATE_RESTRICTIONS = stringSetPreferencesKey("library_auto_update_restrictions")
        private val LIBRARY_UPDATE_LAST_TIMESTAMP = longPreferencesKey(CustomPreferences.appStateKey("library_update_last_timestamp"))
        val NEW_UPDATES_COUNT = intPreferencesKey(CustomPreferences.appStateKey("library_unseen_updates_count"))

        // Categories
        val DEFAULT_CATEGORY = intPreferencesKey("library_default_category")
        val UPDATE_CATEGORIES = stringSetPreferencesKey("library_update_categories")
        val UPDATE_CATEGORIES_EXCLUDE = stringSetPreferencesKey("library_update_categories_exclude")
        private val AUTO_UPDATE_METADATA = booleanPreferencesKey("library_auto_update_metadata")
        private val UPDATE_TITLES = booleanPreferencesKey("library_update_titles")

        private val CATEGORY_NUMBER_OF_ITEMS = booleanPreferencesKey("library_category_number_of_items")
        private val CATEGORY_TABS = booleanPreferencesKey("library_display_category_tabs")
        private val CATEGORIZED_SORT = booleanPreferencesKey("library_categorized_sort")
        val DEFAULT_CATEGORY_FLAGS = longPreferencesKey("library_default_category_flags")
        // The category of the tab last shown
        val LAST_USED_CATEGORY = longPreferencesKey(CustomPreferences.appStateKey("library_last_used_category"))

        const val DEFAULT_CATEGORY_ASK = -1

        // Flags
        private val FILTER_FLAGS = longPreferencesKey("library_filter_flags")
        private val SORT_FLAGS = longPreferencesKey("library_sort_flags")

        override fun transform(preferences: Preferences): LibraryPreferences {
            return LibraryPreferences(
                portraitColumns = preferences[PORTRAIT_COLUMNS] ?: 0,
                landscapeColumns = preferences[LANDSCAPE_COLUMNS] ?: 0,
                autoUpdateInterval = preferences[AUTO_UPDATE_INTERVAL] ?: AutoUpdateIntervalItems.DISABLED,
                autoUpdateRestrictions = preferences[AUTO_UPDATE_RESTRICTIONS] ?: setOf(
                    AutoUpdateRestrictionItems.WIFI
                ),
                filterFlags = LibraryFilter(flags = preferences[FILTER_FLAGS] ?: LibraryFilter.DEFAULT_FILTER),
                sortFlags = LibrarySort(flags = preferences[SORT_FLAGS] ?: LibrarySort.DEFAULT_SORT),
                lastUpdatedTimestamp = preferences[LIBRARY_UPDATE_LAST_TIMESTAMP] ?: 0L,
                newUpdatesCount = preferences[NEW_UPDATES_COUNT] ?: 0,
                defaultCategory = preferences[DEFAULT_CATEGORY] ?: DEFAULT_CATEGORY_ASK,
                updateCategories = preferences[UPDATE_CATEGORIES] ?: emptySet(),
                updateCategoriesExclude = preferences[UPDATE_CATEGORIES_EXCLUDE] ?: emptySet(),
                autoUpdateMetadata = preferences[AUTO_UPDATE_METADATA] ?: false,
                updateTitles = preferences[UPDATE_TITLES] ?: false,
                showCategoryNumberOfItems = preferences[CATEGORY_NUMBER_OF_ITEMS] ?: false,
                showCategoryTabs = preferences[CATEGORY_TABS] ?: true,
                categorizedSort = preferences[CATEGORIZED_SORT] ?: false,
                defaultCategorySortFlags = preferences[DEFAULT_CATEGORY_FLAGS] ?: 0L
            )
        }

        override fun setPrefs(newValue: LibraryPreferences, preferences: MutablePreferences) {
            preferences[PORTRAIT_COLUMNS] = newValue.portraitColumns
            preferences[LANDSCAPE_COLUMNS] = newValue.landscapeColumns
            preferences[AUTO_UPDATE_INTERVAL] = newValue.autoUpdateInterval
            preferences[AUTO_UPDATE_RESTRICTIONS] = newValue.autoUpdateRestrictions
            preferences[FILTER_FLAGS] = newValue.filterFlags.flags
            preferences[SORT_FLAGS] = newValue.sortFlags.flags
            preferences[LIBRARY_UPDATE_LAST_TIMESTAMP] = newValue.lastUpdatedTimestamp
            preferences[NEW_UPDATES_COUNT] = newValue.newUpdatesCount
            preferences[DEFAULT_CATEGORY] = newValue.defaultCategory
            preferences[UPDATE_CATEGORIES] = newValue.updateCategories
            preferences[UPDATE_CATEGORIES_EXCLUDE] = newValue.updateCategoriesExclude
            preferences[AUTO_UPDATE_METADATA] = newValue.autoUpdateMetadata
            preferences[UPDATE_TITLES] = newValue.updateTitles
            preferences[CATEGORY_NUMBER_OF_ITEMS] = newValue.showCategoryNumberOfItems
            preferences[CATEGORY_TABS] = newValue.showCategoryTabs
            preferences[CATEGORIZED_SORT] = newValue.categorizedSort
            preferences[DEFAULT_CATEGORY_FLAGS] = newValue.defaultCategorySortFlags
        }


    }
}



