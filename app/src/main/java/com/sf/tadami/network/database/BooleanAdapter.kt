package com.sf.tadami.network.database

import app.cash.sqldelight.ColumnAdapter

val booleanAdapter = object : ColumnAdapter<Boolean, Long> {
    override fun decode(databaseValue: Long): Boolean = when (databaseValue) {
        0L -> false
        else -> true
    }

    override fun encode(value: Boolean): Long = if (value) 1 else 0
}