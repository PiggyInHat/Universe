package com.example.universe

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class registration : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etSurname: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etPassword2: EditText
    private lateinit var btnRegister: Button
    private lateinit var spinnerFaculty: Spinner
    private lateinit var spinnerGroup: Spinner

    private var selectedFaculty = ""
    private var selectedGroup = ""

    // Сюда сохраняются ID факультетов и их названия, полученные с сайта
    private var facultiesList: List<Pair<String, Int>> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registration)

        etName = findViewById(R.id.editTextName)
        etSurname = findViewById(R.id.editTextSurname)
        etEmail = findViewById(R.id.editTextMail)
        etPassword = findViewById(R.id.editTextPassword)
        etPassword2 = findViewById(R.id.editTextPassword2)
        btnRegister = findViewById(R.id.btnRegister)
        spinnerFaculty = findViewById(R.id.spinner1)
        spinnerGroup = findViewById(R.id.spinner2)

        // 1. Загружаем факультеты с сайта СГУПС в фоне
        loadFacultiesFromSite()

        // 2. Слушаем выбор факультета
        spinnerFaculty.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (facultiesList.isNotEmpty()) {
                    val facultyPair = facultiesList[position]
                    selectedFaculty = facultyPair.first
                    // Загружаем группы для выбранного факультета
                    loadGroupsFromSite(facultyPair.second)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnRegister.setOnClickListener {
            onRegisterClick()
        }
    }

    // Загрузка списка факультетов с сайта
    private fun loadFacultiesFromSite() {
        Toast.makeText(this, "Загрузка списка факультетов...", Toast.LENGTH_SHORT).show()

        Thread {
            try {
                facultiesList = ScheduleParser.getFaculties()
                runOnUiThread {
                    if (facultiesList.isNotEmpty()) {
                        val facultyNames = facultiesList.map { it.first }
                        val adapter = ArrayAdapter(this, R.layout.item_spinner, facultyNames).apply {
                            setDropDownViewResource(R.layout.item_spinner_dropdown)
                        }
                        spinnerFaculty.adapter = adapter
                    } else {
                        Toast.makeText(this, "Не удалось загрузить факультеты", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Ошибка сети. Проверьте интернет", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    // Загрузка списка групп для выбранного факультета
    private fun loadGroupsFromSite(facultyId: Int) {
        // Очищаем спиннер групп, пока грузятся новые
        spinnerGroup.adapter = null
        selectedGroup = ""

        Thread {
            try {
                val groups = ScheduleParser.getGroupsByFacultyId(facultyId)
                runOnUiThread {
                    if (groups.isNotEmpty()) {
                        val adapter = ArrayAdapter(this, R.layout.item_spinner, groups).apply {
                            setDropDownViewResource(R.layout.item_spinner_dropdown)
                        }
                        spinnerGroup.adapter = adapter

                        spinnerGroup.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                                selectedGroup = groups[position]
                            }
                            override fun onNothingSelected(parent: AdapterView<*>?) {
                                selectedGroup = ""
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    // Логика регистрации
    private fun onRegisterClick() {
        val name = etName.text.toString().trim()
        val surname = etSurname.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()
        val password2 = etPassword2.text.toString()

        if (name.isEmpty() || surname.isEmpty() || email.isEmpty() || password.isEmpty() || password2.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Введите корректный email", Toast.LENGTH_SHORT).show()
            return
        }
        if (password.length < 6) {
            Toast.makeText(this, "Пароль минимум 6 символов", Toast.LENGTH_SHORT).show()
            return
        }
        if (password != password2) {
            Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedFaculty.isEmpty() || selectedGroup.isEmpty()) {
            Toast.makeText(this, "Дождитесь загрузки и выберите факультет и группу", Toast.LENGTH_SHORT).show()
            return
        }

        btnRegister.isEnabled = false
        btnRegister.text = "Регистрация..."

        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val userId = authResult.user?.uid
                if (userId == null) {
                    Toast.makeText(this, "Ошибка: нет userId", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val user = User(
                    name = name,
                    surname = surname,
                    email = email,
                    password = password,
                    faculty = selectedFaculty,
                    group = selectedGroup
                )

                db.collection("users").document(userId).set(user.toMap())
                    .addOnSuccessListener {
                        FirebaseAuth.getInstance().signOut()
                        Toast.makeText(this, "Регистрация успешна! Войдите.", Toast.LENGTH_LONG).show()
                        startActivity(Intent(this, login::class.java))
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Ошибка сохранения: ${e.message}", Toast.LENGTH_LONG).show()
                        btnRegister.isEnabled = true
                        btnRegister.text = "Зарегистрироваться"
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка регистрации: ${e.message}", Toast.LENGTH_LONG).show()
                btnRegister.isEnabled = true
                btnRegister.text = "Зарегистрироваться"
            }
    }
}