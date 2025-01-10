package com.idi.vaccinetracker.visit.zscore

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.idi.vaccinetracker.R
import com.idi.vaccinetracker.common.di.AppResources
import com.idi.vaccinetracker.common.domain.entities.Gender
import com.idi.vaccinetracker.common.helpers.dpToPx
import com.idi.vaccinetracker.visit.adapters.OtherSubstanceItemAdapter

class HardcodedWeightForAgeZScore(
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

   override fun setupView(
      view: View,
      listener: OtherSubstanceItemAdapter.AddSubstanceValueListener
   ) {
      val context = view.context
      val linearLayout = createLinearLayout(context)
      val labelTextView = createLabelTextView(context)
      val valueTextView = createValueTextView(context)

      linearLayout.apply {
         addView(labelTextView)
         addView(valueTextView)
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
         text = AppResources(context).getString(R.string.visit_z_score_weight_label)
         layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
         )
         setTypeface(typeface, android.graphics.Typeface.BOLD)
         textSize = 18f
         setTextColor(ContextCompat.getColor(context, android.R.color.black))
      }
   }

   private fun createValueTextView(context: android.content.Context): TextView {
      val textContent = WeightZScoreCalculator(weight, gender, birthDateText).calculateZScoreAndRating() ?: AppResources(context).getString(R.string.visit_z_score_weight_hint)

      return TextView(context).apply {
         text = textContent.toString()
         layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
         ).apply {
            gravity = Gravity.CENTER
         }
         gravity = Gravity.CENTER
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
