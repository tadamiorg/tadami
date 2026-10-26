package com.sf.tadami.data.category

import com.sf.tadami.domain.category.Category

object CategoryMapper {
    fun mapCategory(
        id: Long,
        name: String,
        sort: Long,
        flags: Long
    ): Category = Category(
        id = id,
        name = name,
        order = sort,
        flags = flags
    )
}
