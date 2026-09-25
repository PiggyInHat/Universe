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

    private var selectedFaculty: String = ""
    private var selectedGroup: String = ""

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

        setupFacultySpinner()

        // Слушаем выбор факультета
        spinnerFaculty.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectedFaculty = FacultyData.faculties[position]
                updateGroupSpinner(selectedFaculty)   // ← вызываем метод КЛАССА
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}

        }
        btnRegister.setOnClickListener {
            onRegisterClick()
        }
    }   // ← здесь закрывается onCreate

    // ─────── МЕТОДЫ КЛАССА ───────

    private fun setupFacultySpinner() {
        val facultyAdapter = ArrayAdapter(
            this,
            R.layout.item_spinner,
            FacultyData.faculties
        ).apply {
            setDropDownViewResource(
                R.layout.item_spinner_dropdown
            )
        }
        spinnerFaculty.adapter = facultyAdapter
    }

    // ⬇ ЭТОТ МЕТОД ДОЛЖЕН БЫТЬ ЗДЕСЬ, НА УРОВНЕ КЛАССА!
    private fun updateGroupSpinner(faculty: String) {
        val groups = FacultyData.getGroupsByFaculty(faculty)

        val groupAdapter = ArrayAdapter(
            this,
            R.layout.item_spinner,
            groups
        ).apply {
            setDropDownViewResource(
                R.layout.item_spinner_dropdown
            )
        }
        spinnerGroup.adapter = groupAdapter

        // Слушаем выбор группы
        spinnerGroup.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectedGroup = groups[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedGroup = ""
            }
        }
    }
    private fun onRegisterClick() {
        val name=etName.text.toString().trim()
        val surname = etSurname.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()
        val password2 = etPassword2.text.toString()

        if(name.isEmpty()||surname.isEmpty()||email.isEmpty()||password.isEmpty()||password2.isEmpty()){
            Toast.makeText(this, "Заполните поля", Toast.LENGTH_SHORT).show()
            return
        }
        if(!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()){
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
            Toast.makeText(this, "Выберите факультет и группу", Toast.LENGTH_SHORT).show()
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

                // 4. Создаём объект User и сохраняем в Firestore
                val user = User(
                    name = name,
                    surname = surname,
                    email = email,
                    password = password,
                    faculty = selectedFaculty,
                    group = selectedGroup
                )

                db.collection("users")
                    .document(userId)
                    .set(user.toMap())
                    .addOnSuccessListener {

                        FirebaseAuth.getInstance().signOut()

                        Toast.makeText(this, "Регистрация успешна! Войдите.", Toast.LENGTH_LONG).show()
                        startActivity(Intent(this, login::class.java))
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Ошибка сохранения: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка регистрации: ${e.message}", Toast.LENGTH_LONG).show()
                // РАЗБЛОКИРУЕМ КНОПКУ при ошибке
                btnRegister.isEnabled = true
                btnRegister.text = "Зарегистрироваться"
            }

    }


}   // ← здесь закрывается класс registration