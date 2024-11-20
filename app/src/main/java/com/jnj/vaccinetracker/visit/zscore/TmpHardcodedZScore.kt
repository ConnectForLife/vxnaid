package com.jnj.vaccinetracker.visit.zscore

import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.di.AppResources
import com.jnj.vaccinetracker.common.domain.entities.Gender
import com.jnj.vaccinetracker.common.helpers.dpToPx
import com.jnj.vaccinetracker.visit.adapters.OtherSubstanceItemAdapter

//temporary class for handling Weight for Age as dropdown, should be calculated in the future using class HardcodedWeightForAgeZScore

class TmpHardcodedZScore(
   name: String,
   gender: Gender,
   birthDateText: String,
) : HardcodedZScore(name, gender, birthDateText) {

   companion object {
      const val SEVERELY_UNDERWEIGHT = "Severely Underweight"
      const val UNDERWEIGHT = "Underweight"
      const val NORMAL = "Normal"
      const val OVERWEIGHT = "Overweight"
      const val OBESE = "Obese"
   }

   private var labelTextView: TextView? = null

   override fun getValue(): String? = zScore
   override fun isEmpty(): Boolean {
      return zScore.isNullOrEmpty()
   }

   override fun onEmpty(): () -> Unit {
      return {labelTextView?.error = "Fill data"}
   }

   override fun onNotEmpty(): () -> Unit {
      return {labelTextView?.error = null}
   }

   override fun setupView(
      view: View,
      listener: OtherSubstanceItemAdapter.AddSubstanceValueListener
   ) {
      val context = view.context
      val linearLayout = createLinearLayout(context)
      labelTextView = createLabelTextView(context)
      val dropdownMenu = createConfiguredDropdownMenu(context, listener)

      linearLayout.apply {
         addView(labelTextView)
         addView(dropdownMenu)
      }

      addLinearLayoutToViewGroup(view, linearLayout)
   }

   private fun createLinearLayout(context: android.content.Context): LinearLayout {
      return LinearLayout(context).apply {
         orientation = LinearLayout.VERTICAL
         layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
         )
         setPadding(16.dpToPx, 16.dpToPx, 16.dpToPx, 16.dpToPx)
      }
   }

   private fun createLabelTextView(context: android.content.Context): TextView {
      return TextView(context).apply {
         text = AppResources(context).getString(R.string.visit_z_score_label)
         layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
         )
         setTypeface(typeface, android.graphics.Typeface.BOLD)
         textSize = 18f
         setTextColor(ContextCompat.getColor(context, android.R.color.black))
      }
   }

   private fun createConfiguredDropdownMenu(
      context: android.content.Context,
      listener: OtherSubstanceItemAdapter.AddSubstanceValueListener
   ): com.google.android.material.textfield.TextInputLayout {
      val options = listOf(
         SEVERELY_UNDERWEIGHT,
         UNDERWEIGHT,
         NORMAL, // Default option
         OVERWEIGHT,
         OBESE
      )

      val themedContext = ContextThemeWrapper(
         context,
         com.google.android.material.R.style.Widget_MaterialComponents_TextInputLayout_OutlinedBox_Dense_ExposedDropdownMenu
      )

      return com.google.android.material.textfield.TextInputLayout(themedContext).apply {
         layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
         )

         val autoCompleteTextView = AutoCompleteTextView(context).apply {
            id = View.generateViewId()
            layoutParams = LinearLayout.LayoutParams(
               LinearLayout.LayoutParams.MATCH_PARENT,
               LinearLayout.LayoutParams.WRAP_CONTENT
            )
            inputType = android.text.InputType.TYPE_NULL

            setAdapter(
               ArrayAdapter(
                  context,
                  android.R.layout.simple_dropdown_item_1line,
                  options
               )
            )

            getValue()?.let {
               setText(it, false) // 'false' means don't call 'onTextChanged' on this action
            }

            setOnItemClickListener { _, _, position, _ ->
               val selectedItem = options[position]
               zScore = selectedItem
               notifyListener(listener)
            }
         }

         addView(autoCompleteTextView)
      }
   }

   private fun notifyListener(listener: OtherSubstanceItemAdapter.AddSubstanceValueListener) {
      getValue()?.let {
         listener.addOtherSubstance(conceptName, it)
      }
   }

   private fun addLinearLayoutToViewGroup(view: View, linearLayout: LinearLayout) {
      if (view is ViewGroup) {
         view.addView(linearLayout)
      } else {
         throw IllegalArgumentException("Provided view is not a ViewGroup")
      }
   }
}
