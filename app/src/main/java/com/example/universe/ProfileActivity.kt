package com.example.universe

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var tvFullName: TextView
    private lateinit var tvGroup: TextView
    private lateinit var tvFaculty: TextView
    private lateinit var tvEmail: TextView
    private lateinit var btnLogout: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        tvFullName = findViewById(R.id.tvFullName)
        tvGroup = findViewById(R.id.tvGroup)
        tvFaculty = findViewById(R.id.tvFaculty)
        tvEmail = findViewById(R.id.tvEmail)
        btnLogout = findViewById(R.id.btnLogout)

        // Загрузка данных из Firestore
        loadProfile()
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }
        // Кнопка "Выйти"
        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, login::class.java))
            finish()
        }

        // Bottom Navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_profile

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_schedule -> {
                    startActivity(Intent(this, ScheduleActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_profile -> true
                else -> false
            }
        }
    }

    private fun loadProfile() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            startActivity(Intent(this, login::class.java))
            finish()
            return
        }

        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val name = document.getString("name") ?: ""
                    val surname = document.getString("surname") ?: ""
                    val group = document.getString("group") ?: ""
                    val faculty = document.getString("faculty") ?: ""
                    val email = document.getString("email") ?: ""

                    tvFullName.text = "$name $surname"
                    tvGroup.text = group
                    tvFaculty.text = faculty
                    tvEmail.text = email
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка загрузки: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    override fun onResume() {
        super.onResume()
        // Принудительно подсвечиваем "Профиль" при возврате
        findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav).selectedItemId = R.id.nav_profile
    }
}