package com.example.universe

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

object ScheduleParser {

    // Базовая функция для подключения к сайту с правильной кодировкой
    private fun getDocument(url: String): Document {
        return Jsoup.connect(url)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
            .header("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
            .header("Referer", "https://www.stu.ru/education/raspisanie")
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .timeout(15000)
            // ⚠️ ВАЖНО: Сайт использует кодировку windows-1251, принудительно указываем её
            .postDataCharset("windows-1251")
            .get()
    }

    // 1. Получаем список всех факультетов с сайта
    fun getFaculties(): List<Pair<String, Int>> {
        // Открываем главную страницу расписания, чтобы получить список факультетов
        val url = "https://www.stu.ru/education/raspisanie"
        val doc = getDocument(url)

        val result = mutableListOf<Pair<String, Int>>()
        val options = doc.select("select#faculty option")

        for (option in options) {
            val id = option.attr("value")
            var name = option.text().trim() // Берем полное имя с сайта

            // ⬇⬇⬇ УМНОЕ СОКРАЩЕНИЕ НАЗВАНИЙ ⬇⬇⬇
            name = when {
                name.contains("Бизнес-информатика", ignoreCase = true) -> "ФБИ"
                name.contains("Мосты и тоннели", ignoreCase = true) -> "МТ"
                name.contains("Инженерно-экономический", ignoreCase = true) -> "ИЭФ"
                name.contains("Мировая экономика и право", ignoreCase = true) -> "МЭП"
                name.contains("Промышленное и гражданское строительство", ignoreCase = true) -> "ПГС"
                name.contains("Публичное управление", ignoreCase = true) -> "ПУСК"
                name.contains("Транспортное строительство", ignoreCase = true) -> "ТС"
                name.contains("Управление процессами перевозок", ignoreCase = true) -> "УПП"
                name.contains("Управление транспортно-технологическими", ignoreCase = true) -> "УТТК"
                name.contains("Заочный", ignoreCase = true) -> "Заочный"
                else -> name // Если это новый факультет, оставляем как есть
            }
            // ⬆⬆⬆ КОНЕЦ УМНОГО СОКРАЩЕНИЯ ⬆⬆⬆

            // Пропускаем пустые значения
            if (id.isNotEmpty() && name.isNotEmpty() && id != "0") {
                result.add(Pair(name, id.toIntOrNull() ?: 0))
            }
        }
        return result
    }

    // 2. Получаем список всех групп для конкретного факультета
    fun getGroupsByFacultyId(facultyId: Int): List<String> {
        // Запрашиваем страницу с параметром faculty, чтобы подгрузились группы
        val url = "https://www.stu.ru/education/raspisanie_groups.php" +
                "?faculty=$facultyId&cur_year=2026"

        val doc = getDocument(url)

        val result = mutableListOf<String>()
        val options = doc.select("select#group option")

        for (option in options) {
            val name = option.text().trim()
            // Отсеиваем пустые строки и заглушки
            if (name.isNotEmpty() && !name.contains("Выберите")) {
                result.add(name)
            }
        }
        return result
    }

    // 3. Получаем ID группы по её названию (для расписания)
    fun getGroupId(facultyId: Int, groupName: String): String? {
        val url = "https://www.stu.ru/education/raspisanie_groups.php" +
                "?faculty=$facultyId&cur_year=2026"

        val doc = getDocument(url)

        val options = doc.select("select#group option")
        for (option in options) {
            if (option.text().trim().equals(groupName, ignoreCase = true)) {
                return option.attr("value")
            }
        }
        return null
    }

    // 4. Получаем само расписание (парсинг таблицы)
    fun getSchedule(groupId: String, dateFrom: String): List<ScheduleItem> {
        val url = "https://www.stu.ru/education/raspisanie_table.php" +
                "?group=$groupId" +
                "&lecturer_oid=0" +
                "&auditorium_oid=0" +
                "&date_from=$dateFrom" +
                "&cur_year=2026"

        val doc = getDocument(url)

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

            for (i in 0 until minOf(cells.size, 3)) {
                val text = cells[i].text().trim()
                if (timeRegex.matches(text)) {
                    timeText = text
                    timeIndex = i
                    break
                }
            }

            // Если времени нет — это либо заголовок, либо строка с датой без пар
            if (timeText.isEmpty()) {
                val dateText = cells[0].text().trim()
                if (dateText.contains(".")) {
                    val parts = dateText.split("\n")
                    currentDate = parts.getOrNull(0)?.trim() ?: ""
                    currentDay = parts.getOrNull(1)?.trim() ?: ""
                }
                continue
            }

            if (timeIndex == 1) {
                val dateText = cells[0].text().trim()
                if (dateText.contains(".")) {
                    val parts = dateText.split("\n")
                    currentDate = parts.getOrNull(0)?.trim() ?: ""
                    currentDay = parts.getOrNull(1)?.trim() ?: ""
                }
            }

            // 2. Все остальные данные идут ПОСЛЕ времени
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

            // Очистка предмета от дефиса
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