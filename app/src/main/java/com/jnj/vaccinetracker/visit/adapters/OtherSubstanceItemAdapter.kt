package com.jnj.vaccinetracker.visit.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.domain.entities.RegisterParticipant
import com.jnj.vaccinetracker.participantflow.model.ParticipantSummaryUiModel
import com.jnj.vaccinetracker.visit.model.OtherSubstanceDataModel
import com.jnj.vaccinetracker.visit.zscore.HardcodedZScore
import com.soywiz.klock.DateFormat

class OtherSubstanceItemAdapter(
    private val items: MutableList<OtherSubstanceDataModel>,
    private val listener: AddSubstanceValueListener,
    private val participant: ParticipantSummaryUiModel? = null,
    private val registerParticipant: RegisterParticipant? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var otherSubstanceValues: MutableMap<String, String>? = mutableMapOf()
        set(value) {
            field = value
            // Notify only the HardcodedZScoreViewHolder items
            for (i in items.indices) {
                if (getItemViewType(i) == TYPE_HARDCODED_Z_SCORE) {
                    notifyItemChanged(i)
                }
            }
        }

    companion object {
        const val TYPE_TEXT = 0
        const val TYPE_RADIO = 1
        const val TYPE_MULTIPLE_RADIO = 2
        const val TYPE_NUMBER = 3
        const val TYPE_NUMBER_DECIMAL = 4
        const val TYPE_HARDCODED_Z_SCORE = 5
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position].inputType) {
            "radio" -> TYPE_RADIO
            "multipleRadio" -> TYPE_MULTIPLE_RADIO
            "text", "" -> TYPE_TEXT
            "number" -> TYPE_NUMBER
            "numberDecimal" -> TYPE_NUMBER_DECIMAL
            "hardcodedZScore" -> TYPE_HARDCODED_Z_SCORE
            else -> TYPE_TEXT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_RADIO -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.other_substance_radio_input, parent, false)
                RadioViewHolder(view)
            }
            TYPE_MULTIPLE_RADIO -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.other_substance_multiple_radio_input, parent, false)
                MultipleRadioViewHolder(view)
            }
            TYPE_TEXT -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.other_substance_text_input, parent, false)
                TextViewHolder(view)
            }
            TYPE_NUMBER -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.other_substance_number_input, parent, false)
                TextViewHolder(view)
            }
            TYPE_NUMBER_DECIMAL -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.other_substance_number_decimal_input, parent, false)
                TextViewHolder(view)
            }
            TYPE_HARDCODED_Z_SCORE -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.other_substance_hardcoded_input, parent, false)
                HardcodedZScoreViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is TextViewHolder -> holder.bind(items[position])
            is RadioViewHolder -> holder.bind(items[position])
            is MultipleRadioViewHolder -> holder.bind(items[position])
            is HardcodedZScoreViewHolder -> holder.bind(items[position])
        }
    }

    fun updateItemsList(otherSubstances: List<OtherSubstanceDataModel>?) {
        val existingItemsMap = items.associateBy { it.conceptName }
        items.clear()
        if (otherSubstances != null) {
            otherSubstances.forEach { newItem ->
                val existingItem = existingItemsMap[newItem.conceptName]
                if (existingItem != null) {
                    newItem.value = existingItem.value
                }
                items.add(newItem)
            }
        } else {
            items.addAll(emptyList())
        }
        notifyDataSetChanged()
    }

    fun checkIfAnyItemsEmpty(itemsValues: MutableMap<String, String>?, recyclerView: RecyclerView): List<String> {
        val errorList = mutableListOf<String>()

        items.forEachIndexed { index, item ->
            val itemValue = itemsValues?.get(item.conceptName)

            when (getItemViewType(index)) {
                TYPE_TEXT, TYPE_NUMBER, TYPE_NUMBER_DECIMAL -> handleTextInputValidation(index, itemValue, recyclerView, errorList)
                TYPE_RADIO -> handleRadioValidation(index, itemValue, recyclerView, errorList)
                TYPE_HARDCODED_Z_SCORE -> handleZScoreValidation(index, recyclerView, errorList)
            }
        }
        return errorList
    }

    private fun handleTextInputValidation(index: Int, itemValue: String?, recyclerView: RecyclerView, errorList: MutableList<String>) {
        val holder = recyclerView.findViewHolderForAdapterPosition(index) as? TextViewHolder
        val label = holder?.labelTextView?.text
        val errorMessage = "Please fill $label before submitting"

        if (itemValue.isNullOrEmpty()) {
            holder?.inputEditText?.error = errorMessage
            errorList.add(errorMessage)
        } else {
            holder?.inputEditText?.error = null
        }
    }

    private fun handleRadioValidation(index: Int, itemValue: String?, recyclerView: RecyclerView, errorList: MutableList<String>) {
        val holder = recyclerView.findViewHolderForAdapterPosition(index) as? RadioViewHolder
        val label = holder?.labelTextView?.text
        val errorMessage = "Please select $label option before submitting"

        if (itemValue.isNullOrEmpty()) {
            holder?.labelTextView?.error = errorMessage
            errorList.add(errorMessage)
        } else {
            holder?.labelTextView?.error = null
        }
    }

    private fun handleZScoreValidation(index: Int, recyclerView: RecyclerView, errorList: MutableList<String>) {
        val holder = recyclerView.findViewHolderForAdapterPosition(index) as? HardcodedZScoreViewHolder
        val label = holder?.label
        val errorMessage = "Please fill $label before submitting"

        if (holder?.isEmpty == true) {
            holder.onEmpty?.invoke()
            errorList.add(errorMessage)
        } else {
            holder?.onNotEmpty?.invoke()
        }
    }


    inner class TextViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val labelTextView: TextView = itemView.findViewById(R.id.label_otherSubstance)
        val inputEditText: EditText = itemView.findViewById(R.id.editText_otherSubstance)

        fun bind(item: OtherSubstanceDataModel) {
            labelTextView.text = item.label
            inputEditText.setText(item.value)
            inputEditText.addTextChangedListener { editable ->
                val value = editable.toString()
                item.value = value
                listener.addOtherSubstance(item.conceptName, value)
                labelTextView.error = null
            }
        }
    }

    inner class HardcodedZScoreViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val frameLayout: FrameLayout = itemView.findViewById(R.id.frameLayout_hardcoded)
        var label: String = ""
        var isEmpty: Boolean = true
        var onEmpty: (() -> Unit)? = null
        var onNotEmpty: (() -> Unit)? = null
        fun bind(item: OtherSubstanceDataModel) {
            frameLayout.removeAllViews()
            val gender = participant?.gender ?: registerParticipant?.gender!!
            val birthDateText = participant?.birthDateText ?: registerParticipant?.birthDate!!.toDateTime().format(DateFormat.FORMAT_DATE)
            val hardcodedClass: HardcodedZScore =
                HardcodedZScore.fromConceptName(item.conceptName, gender, birthDateText)
            hardcodedClass.setArguments(otherSubstanceValues)
            hardcodedClass.setValueForConceptName(item.conceptName, item.value)
            hardcodedClass.setupView(itemView, listener)
            isEmpty = hardcodedClass.isEmpty()
            onEmpty = hardcodedClass.onEmpty()
            onNotEmpty = hardcodedClass.onNotEmpty()
            label = item.label
        }
    }

    inner class RadioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val labelTextView: TextView = itemView.findViewById(R.id.label_otherSubstance)
        private val radioGroup: RadioGroup = itemView.findViewById(R.id.radioGroup_otherSubstance)

        fun bind(item: OtherSubstanceDataModel) {
            labelTextView.text = item.label
            radioGroup.removeAllViews()

            item.options.forEachIndexed { index, option ->
                val radioButton = RadioButton(itemView.context).apply {
                    text = option
                    id = View.generateViewId()
                    tag = index

                    if (option == item.value) {
                        isChecked = true
                    }
                }
                radioGroup.addView(radioButton)
            }

            radioGroup.setOnCheckedChangeListener { _, checkedId ->
                val selectedRadioButton = radioGroup.findViewById<RadioButton>(checkedId)
                val selectedIndex = selectedRadioButton.tag as Int
                val selectedValue = item.options[selectedIndex]
                item.value = selectedValue
                listener.addOtherSubstance(item.conceptName, selectedValue)
                labelTextView.error = null
            }
        }
    }

    inner class MultipleRadioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val labelTextView: TextView = itemView.findViewById(R.id.label_otherSubstance)
        private val checkboxGroup: GridLayout = itemView.findViewById(R.id.gridLayout_checkBoxGroup)

        fun bind(item: OtherSubstanceDataModel) {
            val selectedValues = item.value?.split(",")?.map { it.trim() }?.toMutableSet() ?: mutableSetOf()
            labelTextView.text = item.label
            checkboxGroup.removeAllViews()

            item.options.forEachIndexed { index, option ->
                val checkBox = CheckBox(itemView.context).apply {
                    text = option
                    id = View.generateViewId()
                    tag = index
                    isChecked = selectedValues.contains(option)
                }

                checkBox.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedValues.add(option)
                    } else {
                        selectedValues.remove(option)
                    }
                    item.value = selectedValues.joinToString(",")
                    listener.addOtherSubstance(item.conceptName, selectedValues.joinToString(","))
                }

                checkboxGroup.addView(checkBox)
            }
        }
    }

    interface AddSubstanceValueListener {
        fun addOtherSubstance(substanceName: String, value: String)
    }
}