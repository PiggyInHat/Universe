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

        // 1. Список сервисов
        val services = listOf(
            ServiceItem("Зачётка",          android.R.drawable.ic_menu_agenda),
            ServiceItem("Учебный план",     android.R.drawable.ic_menu_edit),
            ServiceItem("Справки",          android.R.drawable.ic_menu_save),
            ServiceItem("Контрольный срок", android.R.drawable.ic_menu_month),
            ServiceItem("Пропуск",          android.R.drawable.ic_menu_view),
            ServiceItem("Библиотека",       android.R.drawable.ic_menu_help),
            ServiceItem("Практика",         android.R.drawable.ic_menu_manage),
            ServiceItem("Сотрудники",       android.R.drawable.ic_menu_myplaces)
        )

        // 2. RecyclerView — сетка 3 колонки
        val recycler = findViewById<RecyclerView>(R.id.recyclerServices)
        recycler.layoutManager = GridLayoutManager(this, 3)
        recycler.adapter = ServiceAdapter(services) { item ->
            when (item.name) {
                "Справки" -> {
                    Toast.makeText(this, "Справки — позже", Toast.LENGTH_SHORT).show()
                    // startActivity(Intent(this, SpravkiActivity::class.java))
                }
                "Практика" -> {
                    Toast.makeText(this, "Практика — позже", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Toast.makeText(this, item.name, Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 3. Bottom Navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_home   // подсветить "Главная"
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Мы уже на главной — ничего не делаем
                    true
                }
                R.id.nav_schedule -> {
                    // startActivity(Intent(this, ScheduleActivity::class.java))
                    Toast.makeText(this, "Расписание — позже", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_profile -> {
                    // startActivity(Intent(this, ProfileActivity::class.java))
                    Toast.makeText(this, "Профиль — позже", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
    }
}