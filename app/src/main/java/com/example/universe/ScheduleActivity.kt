package com.example.universe

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule)

        recycler = findViewById(R.id.recyclerSchedule)
        recycler.layoutManager = LinearLayoutManager(this)

        // Навигация
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_schedule
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { startActivity(Intent(this, MainActivity::class.java)); finish(); true }
                R.id.nav_schedule -> true
                R.id.nav_profile -> { startActivity(Intent(this, ProfileActivity::class.java)); finish(); true }
                else -> false
            }
        }

        // 1. Кэш
        val cached = ScheduleCache.loadSchedule(this)
        if (cached.isNotEmpty()) {
            recycler.adapter = ScheduleAdapter(cached)
        }

        // 2. Сайт
        loadFromSite()
    }

    private fun loadFromSite() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                val groupName = document.getString("group") ?: "БИСТ-311"
                fetchSchedule(groupName)
            }
            .addOnFailureListener { fetchSchedule("БИСТ-311") }
    }

    private fun fetchSchedule(groupName: String) {
        // Берем сегодняшнюю дату
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        Thread {
            try {
                val groupId = ScheduleParser.getGroupId(2, groupName)
                if (groupId != null) {
                    val schedule = ScheduleParser.getSchedule(groupId, currentDate)
                    if (schedule.isNotEmpty()) {
                        ScheduleCache.saveSchedule(this, schedule, currentDate)

                        runOnUiThread {
                            // Просто показываем всё как есть
                            recycler.adapter = ScheduleAdapter(schedule)
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Ошибка сети. Показаны старые данные.", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
}