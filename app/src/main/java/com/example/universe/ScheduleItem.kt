package com.example.universe


data class ScheduleItem(
    val date: String,          // "16.10.2026"
    val dayOfWeek: String,     // "Пт"
    val startTime: String,     // "08:30"
    val endTime: String,       // "10:00"
    val subject: String,       // "Мультимедиа технологии"
    val lessonType: String,    // "ЛР", "Л", "ПЗ"
    val auditorium: String,    // "1/461"
    val teacher: String,       // "Уланов Алексей Александрович"
    val group: String          // "БИСТ-311"
) {
    // Номер пары (вычисляется по времени)
    val lessonNumber: Int
        get() = when (startTime) {
            "08:30" -> 1
            "10:15" -> 2
            "12:00" -> 3
            "14:10" -> 4
            "16:00" -> 5
            "17:40" -> 6
            "19:20" -> 7
            else -> 0
        }

    // Расшифровка типа занятия
    val lessonTypeFull: String
        get() = when (lessonType) {
            "Л" -> "Лекция"
            "ЛР" -> "Лабораторная работа"
            "ПЗ" -> "Практическое занятие"
            "С" -> "Семинар"
            else -> lessonType
        }
}