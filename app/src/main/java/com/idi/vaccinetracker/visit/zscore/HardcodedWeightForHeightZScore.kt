package com.idi.vaccinetracker.visit.zscore

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.idi.vaccinetracker.R
import com.idi.vaccinetracker.common.data.models.Constants
import com.idi.vaccinetracker.common.di.AppResources
import com.idi.vaccinetracker.common.domain.entities.Gender
import com.idi.vaccinetracker.common.helpers.dpToPx
import com.idi.vaccinetracker.visit.adapters.OtherSubstanceItemAdapter

// we only use MUACA and weight for age now but this class is being left just in case

class HardcodedWeightForHeightZScore(
   name: String,
   gender: Gender,
   birthDateText: String,
) : HardcodedZScore(name, gender, birthDateText) {

   override fun getValue(): String? = null
   override fun isEmpty(): Boolean {
      return false
   }

   override fun onEmpty(): () -> Unit {
      return {}
   }

   override fun onNotEmpty(): () -> Unit {
      return {}
   }

   override fun setupView(view: View, listener: OtherSubstanceItemAdapter.AddSubstanceValueListener) {
      val context = view.context

      val linearLayout = createLinearLayout(context)
      val labelTextView = createLabelTextView(context)
      val valueTextView = createValueTextView(context)
      val isOedemaCheckBox = createIsOedemaCheckBox(context)

      linearLayout.apply {
         addView(labelTextView)
         addView(valueTextView)
         if (isOedemaCheckBox != null) {
            addView(isOedemaCheckBox)
         }
      }

      addLinearLayoutToViewGroup(view, linearLayout)

      isOedemaCheckBox?.setOnCheckedChangeListener { _, isChecked ->
         listener.addOtherSubstance(Constants.CONCEPT_NAME_IS_OEDEMA_Z_SCORE, isChecked.toString())
      }
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
         text = AppResources(context).getString(R.string.visit_z_score_weight_for_height_label)
         layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
         )
         setTypeface(typeface, android.graphics.Typeface.BOLD)
         textSize = 18f
         setTextColor(ContextCompat.getColor(context, android.R.color.black))
      }
   }

   private fun createValueTextView(context: android.content.Context): TextView {
      val calculator = WeightForHeightZScoreCalculator(weight, height, isOedema, gender, birthDateText)
      val textContent = calculator.calculateZScoreAndRating() ?: AppResources(context).getString(R.string.visit_z_score_weight_for_height_hint)

      return TextView(context).apply {
         text = textContent.toString()
         layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
         ).apply {
            gravity = Gravity.CENTER
         }
         gravity = Gravity.CENTER
         setTextColor(calculator.getTextColorBasedOnZsCoreValue())
      }
   }

   private fun createIsOedemaCheckBox(context: android.content.Context): CheckBox? {
      val calculator = WeightForHeightZScoreCalculator(weight, height, isOedema, gender, birthDateText)
      val shouldDisplayCheckbox = calculator.isOedemaValue()

      return if (shouldDisplayCheckbox) {
         CheckBox(context).apply {
            text = AppResources(context).getString(R.string.with_oedema)
            layoutParams = LinearLayout.LayoutParams(
               LinearLayout.LayoutParams.MATCH_PARENT,
               LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
               gravity = Gravity.CENTER
            }
            gravity = Gravity.CENTER
            isChecked = isOedema.toBoolean()
         }
      } else {
         null // No checkbox needed
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
