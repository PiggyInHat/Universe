package com.example.universe

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScheduleActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var tvTitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule)

        recycler = findViewById(R.id.recyclerSchedule)
        recycler.layoutManager = LinearLayoutManager(this)
        tvTitle = findViewById(R.id.tvTitle)

        // Стрелочка назад
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Кнопка поиска преподавателя
        findViewById<ImageView>(R.id.btnSearch).setOnClickListener {
            showSearchDialog()
        }

        // Навигация
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_schedule
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_schedule -> true
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }

        // 1. Показываем кэш
        val cached = ScheduleCache.loadSchedule(this)
        if (cached.isNotEmpty()) {
            recycler.adapter = ScheduleAdapter(cached)
        }

        // 2. Загружаем с сайта (свою группу)
        loadFromSite()
    }

    private fun loadFromSite() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                val groupName = document.getString("group") ?: "БИСТ-311"
                val facultyName = document.getString("faculty") ?: "ФБИ"
                fetchSchedule(groupName, facultyName)
            }
            .addOnFailureListener {
                fetchSchedule("БИСТ-311", "ФБИ")
            }
    }

    private fun fetchSchedule(groupName: String, facultyName: String) {
        // Возвращаем заголовок на место, если до этого искали преподавателя
        tvTitle.text = "РАСПИСАНИЕ"

        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val facultyId = FacultyMapping.facultyToId[facultyName] ?: 2

        Thread {
            try {
                val groupId = ScheduleParser.getGroupId(facultyId, groupName)
                if (groupId != null) {
                    val schedule = ScheduleParser.getSchedule(groupId, currentDate)
                    if (schedule.isNotEmpty()) {
                        ScheduleCache.saveSchedule(this, schedule, currentDate)
                        runOnUiThread {
                            recycler.adapter = ScheduleAdapter(schedule)
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this, "На этот день пар нет", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this, "Группа не найдена", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Ошибка сети. Показаны старые данные.", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    // --- ЛОГИКА ПОИСКА ПРЕПОДАВАТЕЛЯ ---

    private fun showSearchDialog() {
        val input = EditText(this).apply {
            hint = "Введите фамилию преподавателя..."
            setPadding(40, 40, 40, 40)
        }

        AlertDialog.Builder(this)
            .setTitle("Поиск преподавателя")
            .setView(input)
            .setPositiveButton("Искать") { _, _ ->
                val query = input.text.toString().trim()
                if (query.isNotEmpty()) {
                    performLecturerSearch(query)
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun performLecturerSearch(query: String) {
        Toast.makeText(this, "Поиск...", Toast.LENGTH_SHORT).show()

        Thread {
            try {
                val lecturers = ScheduleParser.searchLecturers(query)

                runOnUiThread {
                    if (lecturers.isEmpty()) {
                        Toast.makeText(this, "Преподаватель не найден", Toast.LENGTH_SHORT).show()
                    } else if (lecturers.size == 1) {
                        // Если найден только один — сразу грузим его расписание
                        loadLecturerSchedule(lecturers[0].first, lecturers[0].second)
                    } else {
                        // Если найдено несколько — показываем список
                        val names = lecturers.map { it.first }.toTypedArray()
                        AlertDialog.Builder(this)
                            .setTitle("Выберите преподавателя")
                            .setItems(names) { _, which ->
                                loadLecturerSchedule(lecturers[which].first, lecturers[which].second)
                            }
                            .show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Ошибка сети", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun loadLecturerSchedule(name: String, lecturerId: String) {
        // ⬇⬇⬇ ФОРМАТИРОВАНИЕ: "Уланов Алексей Александрович" -> "УЛАНОВ А.А." ⬇⬇⬇
        val parts = name.split(" ").filter { it.isNotBlank() }
        val formattedName = if (parts.size >= 3) {
            val surname = parts[0]
            val firstNameInitial = parts[1].firstOrNull()?.let { "$it." } ?: ""
            val patronymicInitial = parts[2].firstOrNull()?.let { "$it." } ?: ""
            "$surname $firstNameInitial$patronymicInitial"
        } else {
            name // Если формат нестандартный, оставляем как есть
        }
        tvTitle.text = formattedName.uppercase()
        // ⬆⬆⬆

        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        Thread {
            try {
                val schedule = ScheduleParser.getScheduleByLecturer(lecturerId, currentDate)

                runOnUiThread {
                    if (schedule.isNotEmpty()) {
                        recycler.adapter = ScheduleAdapter(schedule)
                    } else {
                        Toast.makeText(this, "На этот день пар нет", Toast.LENGTH_SHORT).show()
                        recycler.adapter = null
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Ошибка загрузки", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
    override fun onResume() {
        super.onResume()
        // Принудительно подсвечиваем "Расписание" при возврате
        findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav).selectedItemId = R.id.nav_schedule
    }
}