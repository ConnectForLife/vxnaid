package com.jnj.vaccinetracker.common.ui

import androidx.databinding.BindingAdapter
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*

object DateBindingAdapters {
    
    private val dateFormatDisplay = SimpleDateFormat("EEE, d MMM yyyy", Locale.ENGLISH)
    
    @BindingAdapter("formattedDate")
    @JvmStatic
    fun setFormattedDate(view: TextView, date: Date?) {
        view.text = date?.let { dateFormatDisplay.format(it) } ?: ""
    }
}

