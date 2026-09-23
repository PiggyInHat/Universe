package com.example.universe

object FacultyData {

    val facultyGroups: Map<String, List<String>> = mapOf(
        "ФТС"  to listOf("ФТС-11", "ФТС-12", "ФТС-21", "ФТС-22", "ФТС-31"),
        "УТТК" to listOf("УТТК-11", "УТТК-12", "УТТК-21", "УТТК-22"),
        "МТ"   to listOf("МТ-11", "МТ-12", "МТ-21", "МТ-22"),
        "УПП"  to listOf("УПП-11", "УПП-12", "УПП-21"),
        "ПГС"  to listOf("ПГС-11", "ПГС-12", "ПГС-21", "ПГС-22"),
        "ИЭФ"  to listOf("ИЭФ-11", "ИЭФ-12", "ИЭФ-21"),
        "МЭИП" to listOf("МЭИП-11", "МЭИП-12", "МЭИП-21"),
        "ФБИ"  to listOf("БИСТ-211", "БИСТ-312", "БИСТ-212", "БПИ-211"),
        "ПУСК" to listOf("ПУСК-11", "ПУСК-12", "ПУСК-21")
    )

    val faculties: List<String> = facultyGroups.keys.toList()

    fun getGroupsByFaculty(faculty: String): List<String> {
        return facultyGroups[faculty] ?: emptyList()
    }
}