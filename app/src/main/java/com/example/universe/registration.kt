package com.example.universe

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class registration : AppCompatActivity() {

    private lateinit var spinnerFaculty: Spinner
    private lateinit var spinnerGroup: Spinner

    private var selectedFaculty: String = ""
    private var selectedGroup: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registration)

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
    }   // ← здесь закрывается onCreate

    // ─────── МЕТОДЫ КЛАССА ───────

    private fun setupFacultySpinner() {
        val facultyAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            FacultyData.faculties
        ).apply {
            setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
            )
        }
        spinnerFaculty.adapter = facultyAdapter
    }

    // ⬇ ЭТОТ МЕТОД ДОЛЖЕН БЫТЬ ЗДЕСЬ, НА УРОВНЕ КЛАССА!
    private fun updateGroupSpinner(faculty: String) {
        val groups = FacultyData.getGroupsByFaculty(faculty)

        val groupAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            groups
        ).apply {
            setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
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

}   // ← здесь закрывается класс registration