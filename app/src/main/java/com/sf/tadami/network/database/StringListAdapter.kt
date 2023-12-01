package com.sf.tadami.network.database

import app.cash.sqldelight.ColumnAdapter

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


val longAdapter = object : ColumnAdapter<Long, Long> {
    override fun decode(databaseValue: Long) = databaseValue
    override fun encode(value: Long) = value
}

val floatDoubleAdapter = object : ColumnAdapter<Float, Double> {
    override fun decode(databaseValue: Double) = databaseValue.toFloat()
    override fun encode(value: Float) = value.toDouble()
}