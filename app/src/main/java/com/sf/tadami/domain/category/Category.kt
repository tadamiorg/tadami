package com.sf.tadami.domain.category

data class Category(
    val id: Long,
    val name: String,
    val order: Long,
    // Kept for the per category sort and display settings
    val flags: Long,
) {
    val isSystemCategory: Boolean
        get() = id == UNCATEGORIZED_ID

    companion object {
        /** The Default category, of the anime in no category. Virtual: it isn't stored */
        const val UNCATEGORIZED_ID = 0L

        fun default() = Category(
            id = UNCATEGORIZED_ID,
            name = "",
            order = -1L,
            flags = 0L
        )
    }
}
