package com.example.universe

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScheduleAdapter(
    private val items: List<ScheduleItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_HEADER = 0
    private val TYPE_ITEM = 1

    // Плоский список с заголовками
    private val displayItems = mutableListOf<Any>()

    init {
        var lastDate = ""
        for (item in items) {
            if (item.date != lastDate) {
                displayItems.add(item.date) // Добавляем заголовок даты
                lastDate = item.date
            }
            displayItems.add(item) // Добавляем саму пару
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (displayItems[position] is String) TYPE_HEADER else TYPE_ITEM
    }

    inner class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDateHeader: TextView = view.findViewById(R.id.tvDateHeader)
    }

    inner class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val tvSubject: TextView = view.findViewById(R.id.tvSubject)
        val tvAuditorium: TextView = view.findViewById(R.id.tvAuditorium)
        val tvType: TextView = view.findViewById(R.id.tvType)
        val tvTeacher: TextView = view.findViewById(R.id.tvTeacher)
        val tvGroup: TextView = view.findViewById(R.id.tvGroup)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_date_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_schedule, parent, false)
            ItemViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = displayItems[position]

        if (holder is HeaderViewHolder && item is String) {
            // 1. Настраиваем заголовок даты
            val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            val date = sdf.parse(item)
            val displayFormat = SimpleDateFormat("d MMMM, EEEE", Locale.forLanguageTag("ru"))
            holder.tvDateHeader.text = displayFormat.format(date ?: Date())

            // Все заголовки всегда черные
            holder.tvDateHeader.setTextColor(Color.parseColor("#000000"))

        } else if (holder is ItemViewHolder && item is ScheduleItem) {
            // 2. Заполняем карточку (без даты, она скрыта в XML)
            holder.tvTime.text = "${item.startTime} - ${item.endTime}"
            holder.tvSubject.text = item.subject
            holder.tvAuditorium.text = item.auditorium
            holder.tvType.text = item.lessonTypeFull
            holder.tvTeacher.text = item.teacher
            holder.tvGroup.text = item.group

            // 3. Все пары всегда яркие (без серого цвета для прошедших)
            holder.tvTime.setTextColor(Color.parseColor("#5B4FE9"))
            holder.tvSubject.setTextColor(Color.parseColor("#000000"))
            holder.tvAuditorium.setTextColor(Color.parseColor("#666666"))
            holder.tvType.setTextColor(Color.parseColor("#5B4FE9"))
            holder.tvTeacher.setTextColor(Color.parseColor("#999999"))
            holder.tvGroup.setTextColor(Color.parseColor("#5B4FE9"))
        }
    }

    override fun getItemCount() = displayItems.size
}