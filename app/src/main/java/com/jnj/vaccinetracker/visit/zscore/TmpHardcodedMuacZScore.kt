package com.jnj.vaccinetracker.visit.zscore

import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.di.AppResources
import com.jnj.vaccinetracker.common.domain.entities.Gender
import com.jnj.vaccinetracker.common.helpers.dpToPx
import com.jnj.vaccinetracker.visit.adapters.OtherSubstanceItemAdapter

//temporary class for handling MUACAe as dropdown, should be calculated in the future using class HardcodedMuacaZScore
class TmpHardcodedMuacZScore(
   name: String,
   gender: Gender,
   birthDateText: String,
) : HardcodedZScore(name, gender, birthDateText) {
   companion object {
      const val NORMAL_NUTRITION_STATUS = "Normal Nutrition Status" // green
      const val MODERATE_NUTRITION_STATUS = "Moderate Acute Malnutrition" // yellow
      const val SEVERE_NUTRITION_STATUS = "Severe Acute Malnutrition" // red
   }

   private var labelTextView: TextView? = null

   override fun getValue(): String? = muac

   override fun isEmpty(): Boolean {
      return muac.isNullOrEmpty()
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
      val spinner = createValueSpinner(context)

      linearLayout.apply {
         addView(labelTextView)
         addView(spinner)
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
         text = AppResources(context).getString(R.string.visit_z_score_muac_label)
         layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
         )
         setTypeface(typeface, android.graphics.Typeface.BOLD)
         textSize = 18f
         setTextColor(ContextCompat.getColor(context, android.R.color.black))
      }
   }

   private fun createValueSpinner(context: android.content.Context): Spinner {
      val options = listOf(
         NORMAL_NUTRITION_STATUS,
         MODERATE_NUTRITION_STATUS,
         SEVERE_NUTRITION_STATUS
      )

      val colors = listOf(
         Color.GREEN,
         Color.parseColor("#FFAA00"),
         Color.RED
      )

      return Spinner(context).apply {
         layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
         ).apply {
            gravity = Gravity.CENTER
         }
         gravity = Gravity.CENTER
         setPadding(0, 16, 16, 0)

         adapter = object : ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, options) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
               val view = super.getView(position, convertView, parent) as TextView
               view.setTextColor(colors[position])
               return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
               val view = super.getDropDownView(position, convertView, parent) as TextView
               view.setTextColor(colors[position])
               view.gravity = Gravity.CENTER
               return view
            }
         }.apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
         }
      }
   }

   private fun createTextWatcher(
      listener: OtherSubstanceItemAdapter.AddSubstanceValueListener,
      valueTextView: TextView
   ): TextWatcher {
      return object : TextWatcher {
         override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            // No action needed here
         }

         override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            muac = s?.toString()
            updateValueTextView(valueTextView)
            notifyListener(listener)
         }

         override fun afterTextChanged(s: Editable?) {
            // No action needed here
         }
      }
   }

   private fun updateValueTextView(valueTextView: TextView) {
      val calculator = MuacZScoreCalculator(muac, gender, birthDateText)
      val text = calculator.calculateZScoreAndRating() ?: ""
      valueTextView.text = text.toString()
      valueTextView.setTextColor(calculator.getTextColorBasedOnZsCoreValue())
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
