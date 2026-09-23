package com.example.universe

class User {
    var name: String = ""
    var surname: String = ""
    var email: String = ""
    var password: String = ""
    var faculty: String = ""
    var group: String = ""

    // Пустой конструктор (нужен для Firebase)
    constructor()

    // Конструктор со всеми полями (удобно для создания объекта)
    constructor(
        name: String,
        surname: String,
        email: String,
        password: String,
        faculty: String,
        group: String
    ) {
        this.name = name
        this.surname = surname
        this.email = email
        this.password = password
        this.faculty = faculty
        this.group = group
    }

    // Метод для преобразования в Map (для Firestore)
    fun toMap(): Map<String, Any> {
        return mapOf(
            "name" to name,
            "surname" to surname,
            "email" to email,
            "faculty" to faculty,
            "group" to group
            // ⚠️ Пароль НЕ сохраняем в Firestore — Firebase Auth сам его хранит
        )
    }
}