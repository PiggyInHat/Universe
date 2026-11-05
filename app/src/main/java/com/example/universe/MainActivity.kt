package com.example.universe

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        Thread {
            try {
                val groupId = ScheduleParser.getGroupId(2, "БИСТ-311")
                android.util.Log.d("SCHEDULE", "Group ID: $groupId")

                if (groupId != null) {
                    val schedule = ScheduleParser.getSchedule(groupId, "2026-10-16")
                    android.util.Log.d("SCHEDULE", "Loaded: ${schedule.size} items")

                    for (item in schedule) {
                        android.util.Log.d("SCHEDULE", "${item.date} ${item.startTime} ${item.subject} ${item.auditorium} ${item.teacher}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SCHEDULE", "Error: ${e.message}")
            }
        }.start()

        // 1. Список сервисов
        val services = listOf(
            ServiceItem("Зачётка",          R.drawable.creditpass),
            ServiceItem("Учебный план",     R.drawable.curriculum),
            ServiceItem("Справки",          R.drawable.certificates),
            ServiceItem("Контрольный срок", R.drawable.targetdeadline),
            ServiceItem("Пропуск",          R.drawable.pass),
            ServiceItem("Библиотека",       R.drawable.library),
            ServiceItem("Практика",         R.drawable.practice),
            ServiceItem("Сотрудники",       R.drawable.staff),
            ServiceItem("Обратная связь",   R.drawable.feedback)
        )

        // 2. RecyclerView — сетка 3 колонки
        val recycler = findViewById<RecyclerView>(R.id.recyclerServices)
        recycler.layoutManager = GridLayoutManager(this, 3)
        recycler.adapter = ServiceAdapter(services) { item ->
            when (item.name) {
                "Справки" -> {
                    Toast.makeText(this, "Справки — позже", Toast.LENGTH_SHORT).show()
                }
                "Практика" -> {
                    Toast.makeText(this, "Практика — позже", Toast.LENGTH_SHORT).show()
                }
                "Обратная связь" -> {
                    startActivity(Intent(this, FeedbackActivity::class.java))
                }
                else -> {
                    Toast.makeText(this, item.name, Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 3. Bottom Navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_home
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_schedule -> {
                    startActivity(Intent(this, ScheduleActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }

        // ⬇⬇⬇ ПРОВЕРКА ОБНОВЛЕНИЙ ⬇⬇⬇
        UpdateChecker.checkForUpdate(this) { release ->
            if (release != null) {
                runOnUiThread {
                    androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Доступно обновление ${release.tag_name}")
                        .setMessage("Вышла новая версия приложения. Установить?")
                        .setPositiveButton("Обновить") { _, _ ->
                            val apkAsset = release.assets.find { it.name.endsWith(".apk") }
                            if (apkAsset != null) {
                                UpdateChecker.downloadAndInstall(this, apkAsset.browser_download_url)
                            }
                        }
                        .setNegativeButton("Позже", null)
                        .show()
                }
            }
        }
        // ⬆⬆⬆ КОНЕЦ ПРОВЕРКИ ⬆⬆⬆
    }

    override fun onResume() {
        super.onResume()
        findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav).selectedItemId = R.id.nav_home
    }
}