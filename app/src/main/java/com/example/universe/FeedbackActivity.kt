package com.example.universe

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FeedbackActivity : AppCompatActivity() {

    private lateinit var spinnerType: Spinner
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback)

        // Стрелочка назад
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        spinnerType = findViewById(R.id.spinnerType)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        tvStatus = findViewById(R.id.tvStatus)

        // Настраиваем типы обращений
        val types = listOf(
            "Ошибка в приложении",
            "Предложение по улучшению",
            "Проблема с расписанием",
            "Другое"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, types)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerType.adapter = adapter

        btnSend.setOnClickListener {
            sendFeedback()
        }
    }

    private fun sendFeedback() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Вы не авторизованы", Toast.LENGTH_SHORT).show()
            return
        }

        val type = spinnerType.selectedItem.toString()
        val message = etMessage.text.toString().trim()

        if (message.isEmpty()) {
            Toast.makeText(this, "Введите сообщение", Toast.LENGTH_SHORT).show()
            return
        }

        // Блокируем кнопку и меняем статус
        btnSend.isEnabled = false
        tvStatus.text = "Отправка..."

        val feedbackData = hashMapOf(
            "type" to type,
            "message" to message,
            "userId" to currentUser.uid,
            "status" to "new",
            "timestamp" to System.currentTimeMillis()
        )

        val db = FirebaseFirestore.getInstance()
        db.collection("feedback")
            .add(feedbackData)
            .addOnSuccessListener {
                tvStatus.text = "Отправлено! Спасибо."
                Toast.makeText(this, "Спасибо! Ваше обращение принято.", Toast.LENGTH_LONG).show()
                etMessage.text.clear()
                btnSend.isEnabled = true
            }
            .addOnFailureListener { e ->
                tvStatus.text = "Ошибка отправки"
                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                btnSend.isEnabled = true
            }
    }
}