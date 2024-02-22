package com.sf.tadami.data

import app.cash.sqldelight.ColumnAdapter
import java.util.Date

private const val listOfStringsSeparator = ", "
val listOfStringsAdapter = object : ColumnAdapter<List<String>, String> {
    override fun decode(databaseValue: String) =
        if (databaseValue.isEmpty()) {
            emptyList()
        } else {
            databaseValue.split(listOfStringsSeparator)
        }
    override fun encode(value: List<String>) = value.joinToString(separator = listOfStringsSeparator)
}

val dateColumnAdapter = object : ColumnAdapter<Date, Long> {
    override fun decode(databaseValue: Long): Date = Date(databaseValue)
    override fun encode(value: Date): Long = value.time
}

val longAdapter = object : ColumnAdapter<Long, Long> {
    override fun decode(databaseValue: Long) = databaseValue
    override fun encode(value: Long) = value
}

val floatDoubleAdapter = object : ColumnAdapter<Float, Double> {
    override fun decode(databaseValue: Double) = databaseValue.toFloat()
    override fun encode(value: Float) = value.toDouble()
}