package com.jnj.vaccinetracker.visit.screens

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.helpers.hideKeyboard
import com.jnj.vaccinetracker.common.ui.BaseActivity
import com.jnj.vaccinetracker.common.ui.SyncBanner
import com.jnj.vaccinetracker.databinding.FragmentContraindicationsBinding
import com.jnj.vaccinetracker.participantflow.ParticipantFlowActivity
import com.jnj.vaccinetracker.participantflow.model.ParticipantSummaryUiModel
import com.jnj.vaccinetracker.visit.VisitActivity
import com.jnj.vaccinetracker.visit.dialog.RescheduleVisitDialog
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
class ContraindicationsActivity : BaseActivity() {
   private val participant: ParticipantSummaryUiModel by lazy {
      intent.getParcelableExtra(EXTRA_PARTICIPANT)!!
   }
   private val newRegisteredParticipant: Boolean by lazy {
      intent.getBooleanExtra(EXTRA_TYPE, false)
   }
   private val viewModel: ContraindicationsViewModel by viewModels { viewModelFactory }
   private lateinit var binding: FragmentContraindicationsBinding
   private var errorSnackbar: Snackbar? = null

   companion object {
      const val TAG_DIALOG_RESCHEDULE_VISIT = "rescheduleVisitDialog"
      private const val EXTRA_PARTICIPANT = "participant"
      private const val EXTRA_TYPE = "newParticipantRegistration"

      fun create(context: Context, participant: ParticipantSummaryUiModel, newRegisteredParticipant: Boolean): Intent {
         return Intent(context, ContraindicationsActivity::class.java).apply {
            putExtra(EXTRA_PARTICIPANT, participant)
            putExtra(EXTRA_TYPE, newRegisteredParticipant)
         }
      }
   }

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      binding = DataBindingUtil.setContentView(this, R.layout.fragment_contraindications)
      binding.viewModel = viewModel
      binding.lifecycleOwner = this

      viewModel.errorMessage.observe(this) { errorMessage ->
         errorSnackbar?.dismiss()

         if (errorMessage == null) return@observe

         errorSnackbar = Snackbar
            .make(binding.root, errorMessage, Snackbar.LENGTH_INDEFINITE)
            .setAction(R.string.general_label_retry) {
               errorSnackbar?.dismiss()
               RescheduleVisitDialog.create(participant = participant)
                  .show(supportFragmentManager, TAG_DIALOG_RESCHEDULE_VISIT)
            }.also {
               it.show()
            }
      }

      setupClickListeners()
      supportActionBar?.setDisplayHomeAsUpEnabled(true)
   }

   private fun setupClickListeners() {
      binding.root.setOnClickListener {
         currentFocus?.hideKeyboard()
      }
      binding.btnYes.setOnClickListener {
         RescheduleVisitDialog.create(participant = participant)
            .show(supportFragmentManager, TAG_DIALOG_RESCHEDULE_VISIT)
      }
      binding.btnNo.setOnClickListener {
         startParticipantVisit(participant, newRegisteredParticipant)
      }
   }

   private fun startParticipantVisit(participant: ParticipantSummaryUiModel, newRegisteredParticipant: Boolean) {
      lifecycleScope.launch {
         startActivity(VisitActivity.create(this@ContraindicationsActivity, participant, newRegisteredParticipant))
         setForwardAnimation()
      }
   }

   override fun onSupportNavigateUp(): Boolean {
      onBackPressed()
      return true
   }

   override fun onBackPressed() {
      if (newRegisteredParticipant) {
         lifecycleScope.launch {
            val intent = ParticipantFlowActivity.create(this@ContraindicationsActivity)
            intent.putExtra(Constants.CALL_NAVIGATE_TO_MATCH_SCREEN, true)
            intent.putExtra(Constants.PARTICIPANT_MATCH_ID, participant.participantId)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finishAffinity()
         }
      } else {
         super.onBackPressed()
      }
   }


   override val syncBanner: SyncBanner
      get() = binding.syncBanner
}

