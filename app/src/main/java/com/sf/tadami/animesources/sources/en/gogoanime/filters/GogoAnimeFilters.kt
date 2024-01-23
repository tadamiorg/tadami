package com.sf.tadami.animesources.sources.en.gogoanime.filters

import com.sf.tadami.source.model.AnimeFilter
import com.sf.tadami.source.model.AnimeFilterList

object GogoAnimeFilters {
    open class QueryPartFilter(
        displayName: String,
        private val vals: Array<Pair<String, String>>,
    ) : AnimeFilter.Select(
        displayName,
        vals.map { it.first }.toTypedArray(),
    ) {
        fun toQueryPart() = vals[state].second
    }


    open class CheckBoxFilterList(name: String, val pairs: Array<Pair<String, String>>) :
        AnimeFilter.CheckBoxGroup(name, pairs.map { CheckBoxVal(it.first, false) })

    private class CheckBoxVal(name: String, state: Boolean = false) : AnimeFilter.CheckBox(name, state)

    private inline fun <reified R> AnimeFilterList.asQueryPart(): String {
        return (getFirst<R>() as QueryPartFilter).toQueryPart()
    }

    private inline fun <reified R> AnimeFilterList.getFirst(): R {
        return first { it is R } as R
    }

    private inline fun <reified R> AnimeFilterList.parseCheckbox(
        options: Array<Pair<String, String>>,
        name: String,
    ): String {
        return (getFirst<R>() as CheckBoxFilterList).state
            .filter { it.state }
            .map { checkbox -> options.find { it.first == checkbox.name }!!.second }
            .filter(String::isNotBlank)
            .joinToString("&") { "$name[]=$it" }
    }

    class GenreSearchFilter : CheckBoxFilterList("Genre", GogoAnimeFiltersData.GENRE_SEARCH_LIST)
    class CountrySearchFilter : CheckBoxFilterList("Country", GogoAnimeFiltersData.COUNTRY_SEARCH_LIST)
    class SeasonSearchFilter : CheckBoxFilterList("Season", GogoAnimeFiltersData.SEASON_SEARCH_LIST)
    class YearSearchFilter : CheckBoxFilterList("Year", GogoAnimeFiltersData.YEAR_SEARCH_LIST)
    class LanguageSearchFilter : CheckBoxFilterList("Language", GogoAnimeFiltersData.LANGUAGE_SEARCH_LIST)
    class TypeSearchFilter : CheckBoxFilterList("Type", GogoAnimeFiltersData.TYPE_SEARCH_LIST)
    class StatusSearchFilter : CheckBoxFilterList("Status", GogoAnimeFiltersData.STATUS_SEARCH_LIST)
    class SortSearchFilter : QueryPartFilter("Sort by", GogoAnimeFiltersData.SORT_SEARCH_LIST)

    class GenreFilter : QueryPartFilter("Genre", GogoAnimeFiltersData.GENRE_LIST)
    class RecentFilter : QueryPartFilter("Recent episodes", GogoAnimeFiltersData.RECENT_LIST)
    class SeasonFilter : QueryPartFilter("Season", GogoAnimeFiltersData.SEASON_LIST)

    val FILTER_LIST get() = AnimeFilterList(
        AnimeFilter.Header("Advanced search"),
        GenreSearchFilter(),
        CountrySearchFilter(),
        SeasonSearchFilter(),
        YearSearchFilter(),
        LanguageSearchFilter(),
        TypeSearchFilter(),
        StatusSearchFilter(),
        SortSearchFilter(),
        AnimeFilter.Header("Select sub-page"),
        AnimeFilter.Header("Note: Ignores search & other filters"),
        GenreFilter(),
        RecentFilter(),
        SeasonFilter(),
    )

    data class FilterSearchParams(
        val filter: String = "",
        val genre: String = "",
        val recent: String = "",
        val season: String = "",
    )

    internal fun getSearchParameters(filters: AnimeFilterList): FilterSearchParams {
        if (filters.isEmpty()) return FilterSearchParams()

        val filter = buildList {
            add(filters.parseCheckbox<GenreSearchFilter>(GogoAnimeFiltersData.GENRE_SEARCH_LIST, "genre"))
            add(filters.parseCheckbox<CountrySearchFilter>(GogoAnimeFiltersData.GENRE_SEARCH_LIST, "country"))
            add(filters.parseCheckbox<SeasonSearchFilter>(GogoAnimeFiltersData.SEASON_SEARCH_LIST, "season"))
            add(filters.parseCheckbox<YearSearchFilter>(GogoAnimeFiltersData.YEAR_SEARCH_LIST, "year"))
            add(filters.parseCheckbox<LanguageSearchFilter>(GogoAnimeFiltersData.LANGUAGE_SEARCH_LIST, "language"))
            add(filters.parseCheckbox<TypeSearchFilter>(GogoAnimeFiltersData.TYPE_SEARCH_LIST, "type"))
            add(filters.parseCheckbox<StatusSearchFilter>(GogoAnimeFiltersData.STATUS_SEARCH_LIST, "status"))
            add("sort=${filters.asQueryPart<SortSearchFilter>()}")
        }.filter(String::isNotBlank).joinToString("&")

        return FilterSearchParams(
            filter,
            filters.asQueryPart<GenreFilter>(),
            filters.asQueryPart<RecentFilter>(),
            filters.asQueryPart<SeasonFilter>(),
        )
    }
}