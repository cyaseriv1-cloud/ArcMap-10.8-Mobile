package com.terraaxis.core.model

enum class FieldType {
    STRING,
    INTEGER,
    DOUBLE,
    DATE,
    BOOLEAN
}

data class Field(
    val name: String,
    val type: FieldType,
    val alias: String = name,
    val length: Int = 254
)
