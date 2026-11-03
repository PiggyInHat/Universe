package com.example.universe

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class login : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvGoToRegister: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // АВТО-ВХОД
        if (FirebaseAuth.getInstance().currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        etEmail = findViewById(R.id.etLoginEmail)
        etPassword = findViewById(R.id.etLoginPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvGoToRegister = findViewById(R.id.tvGoToRegister)

        btnLogin.setOnClickListener { loginUser() }

        tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, registration::class.java))
        }
    }

    private fun loginUser() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Введите email и пароль", Toast.LENGTH_SHORT).show()
            return
        }

        btnLogin.isEnabled = false
        btnLogin.text = "Вход..."

        FirebaseAuth.getInstance()
            .signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                Toast.makeText(this, "Вход выполнен!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                // ⬇⬇⬇ ПРАВИЛЬНЫЕ ОШИБКИ ДЛЯ ВХОДА ⬇⬇⬇
                val errorMessage = when {
                    // 1. Пользователь с таким email не найден
                    e.message?.contains("no user record") == true ||
                            e.message?.contains("user-not-found") == true ->
                        "Пользователь с таким email не найден. Зарегистрируйтесь."

                    // 2. Пароль неверный
                    e.message?.contains("password is invalid") == true ||
                            e.message?.contains("wrong-password") == true ->
                        "Неверный пароль"

                    // 3. Некорректный формат email
                    e.message?.contains("badly formatted") == true ||
                            e.message?.contains("invalid-email") == true ->
                        "Введите корректный email"

                    // 4. Проблемы с интернетом
                    e.message?.contains("network error") == true ||
                            e.message?.contains("network-request-failed") == true ->
                        "Ошибка сети. Проверьте интернет"

                    // 5. Слишком много попыток
                    e.message?.contains("too many requests") == true ||
                            e.message?.contains("too-many-requests") == true ->
                        "Слишком много попыток. Попробуйте позже"

                    // 6. Все остальные ошибки
                    else ->
                        "Не удалось войти. Попробуйте позже"
                }

                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()

                // ⬇⬇⬇ ВОЗВРАЩАЕМ КНОПКУ ВХОДА В РАБОЧЕЕ СОСТОЯНИЕ ⬇⬇⬇
                btnLogin.isEnabled = true
                btnLogin.text = "Войти"
            }
    }
}