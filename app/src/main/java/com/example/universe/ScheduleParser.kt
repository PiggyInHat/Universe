package com.example.universe

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

object ScheduleParser {

    fun getGroupId(facultyId: Int, groupName: String): String? {
        val url = "https://www.stu.ru/education/raspisanie_groups.php" +
                "?faculty=$facultyId&cur_year=2026"

        val doc: Document = Jsoup.connect(url)
            .header("Referer", "https://www.stu.ru/education/raspisanie")
            .header("X-Requested-With", "XMLHttpRequest")
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .timeout(10000)
            .get()

        val options = doc.select("select#group option")
        for (option in options) {
            if (option.text().trim().equals(groupName, ignoreCase = true)) {
                return option.attr("value")
            }
        }
        return null
    }

    fun getSchedule(groupId: String, dateFrom: String): List<ScheduleItem> {
        val url = "https://www.stu.ru/education/raspisanie_table.php" +
                "?group=$groupId" +
                "&lecturer_oid=0" +
                "&auditorium_oid=0" +
                "&date_from=$dateFrom" +
                "&cur_year=2026"

        val doc: Document = Jsoup.connect(url)
            .header("Referer", "https://www.stu.ru/education/raspisanie")
            .header("X-Requested-With", "XMLHttpRequest")
            .userAgent("Mozilla/5.0")
            .timeout(15000)
            .get()

        val table = doc.select("table.itwoa").first() ?: return emptyList()
        val rows = table.select("tr")
        val result = mutableListOf<ScheduleItem>()

        var currentDate = ""
        var currentDay = ""

        // Регулярное выражение для поиска времени (например, "08:30 - 10:00")
        val timeRegex = Regex("""\d{2}:\d{2}\s*-\s*\d{2}:\d{2}""")

        for (row in rows) {
            val cells = row.select("td")
            if (cells.isEmpty()) continue

            // 1. Ищем время в строке. Оно может быть в cells[0] или cells[1]
            var timeText = ""
            var timeIndex = -1

            for (i in 0 until minOf(cells.size, 3)) { // Проверяем только первые 3 ячейки
                val text = cells[i].text().trim()
                if (timeRegex.matches(text)) {
                    timeText = text
                    timeIndex = i
                    break
                }
            }

            // Если времени нет — это либо заголовок, либо строка с датой без пар
            if (timeText.isEmpty()) {
                // Попробуем найти дату в первой ячейке
                val dateText = cells[0].text().trim()
                if (dateText.contains(".")) {
                    val parts = dateText.split("\n")
                    currentDate = parts.getOrNull(0)?.trim() ?: ""
                    currentDay = parts.getOrNull(1)?.trim() ?: ""
                }
                continue
            }

            // Если мы нашли время в cells[0], значит, даты в этой строке нет (она сверху)
            // Если время в cells[1], значит, дата есть в cells[0]
            if (timeIndex == 1) {
                val dateText = cells[0].text().trim()
                if (dateText.contains(".")) {
                    val parts = dateText.split("\n")
                    currentDate = parts.getOrNull(0)?.trim() ?: ""
                    currentDay = parts.getOrNull(1)?.trim() ?: ""
                }
            }

            // 2. Все остальные данные идут ПОСЛЕ времени
            // Сдвигаем индексы в зависимости от того, где было время
            val offset = timeIndex + 1

            val subject = cells.getOrNull(offset)?.text()?.trim() ?: ""
            val lessonType = cells.getOrNull(offset + 1)?.text()?.trim() ?: ""
            val auditorium = cells.getOrNull(offset + 2)?.text()?.trim() ?: ""
            val teacher = cells.getOrNull(offset + 3)?.text()?.trim() ?: ""
            val group = cells.getOrNull(offset + 4)?.text()?.trim() ?: ""

            // Разбор времени
            val timeParts = timeText.split("-")
            val startTime = timeParts.getOrNull(0)?.trim() ?: ""
            val endTime = timeParts.getOrNull(1)?.trim() ?: ""

            // Очистка предмета от дефиса (если он приклеился)
            var cleanSubject = subject
            if (cleanSubject.endsWith("-")) {
                cleanSubject = cleanSubject.dropLast(1).trim()
            }

            // Если группа пустая, но в преподавателе есть подчеркивание — вытаскиваем оттуда
            var finalGroup = group
            if (finalGroup.isEmpty() && teacher.contains("БИСТ")) {
                finalGroup = teacher.substringAfterLast(" ").trim()
            }

            result.add(
                ScheduleItem(
                    date = currentDate,
                    dayOfWeek = currentDay,
                    startTime = startTime,
                    endTime = endTime,
                    subject = cleanSubject,
                    lessonType = lessonType,
                    auditorium = auditorium,
                    teacher = teacher,
                    group = finalGroup
                )
            )
        }

        return result
    }
}