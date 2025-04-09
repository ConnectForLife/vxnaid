package com.jnj.vaccinetracker.login

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.helpers.hideKeyboard
import com.jnj.vaccinetracker.common.ui.BaseActivity
import com.jnj.vaccinetracker.databinding.ActivityLoginBinding
import com.jnj.vaccinetracker.participantflow.ParticipantFlowActivity
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class LoginActivity : BaseActivity() {

    companion object {
        private const val TAG_SETTINGS_DIALOG = "SettingsDialog"
        private const val TAG_UPDATE_DIALOG = "UpdateDialog"

        fun create(context: Context): Intent {
            return Intent(context, LoginActivity::class.java)
        }
    }

    private val loginActivityMenuHelper by lazy {
        LoginActivityMenuHelper(supportFragmentManager)
    }

    private val viewModel: LoginViewModel by viewModels { viewModelFactory }
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        viewModel.init(true)

        // Observe selected visit place
        viewModel.selectedVisitPlace.observe(this) { visitPlace ->
            if (visitPlace == Constants.VISIT_PLACE_OUTREACH) {
                OutreachNameDialogFragment.newInstance(viewModel).show(supportFragmentManager, "OutreachNameDialog")
            }
        }

        binding.btnLogin.setOnClickListener { login() }
        binding.editPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                login()
                true
            } else {
                false
            }
        }

        val visitPlaces = listOf(
            Constants.VISIT_PLACE_STATIC,
            Constants.VISIT_PLACE_OUTREACH,
            Constants.VISIT_PLACE_SCHOOL
        )
        val adapter = ArrayAdapter(
            this,
            R.layout.item_dropdown,
            visitPlaces
        )
        binding.dropdownLoginVisitPlace.setAdapter(adapter)
        binding.dropdownLoginVisitPlace.setOnItemClickListener { _, _, position, _ ->
            viewModel.onVisitPlaceSelected(visitPlaces[position])
        }

        binding.root.setOnClickListener { hideKeyboard() }
        observeViewModel(this)
    }

    private fun observeViewModel(lifecycleOwner: LifecycleOwner) {
        viewModel.loginCompleted
            .onEach {
                onLoginCompleted()
            }
            .launchIn(lifecycleOwner.lifecycleScope)
    }

    private fun login() {
        val username = binding.editUsername.text.toString()
        val password = binding.editPassword.text.toString()
        val visitPlace = binding.dropdownLoginVisitPlace.text.toString()
        val outreachName = viewModel.outreachName.value ?: ""
        viewModel.login(username, password, visitPlace, outreachName)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun onLoginCompleted() {
        startActivity(ParticipantFlowActivity.create(this))
        finish()
    }

    private fun saveVisitPlaceToMemory(visitPlace: String) {
        val sharedPreferences = getSharedPreferences(Constants.USER_PREFERENCES_FILE_NAME, MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(Constants.VISIT_PLACE_FILE_KEY, visitPlace)
        editor.apply()
    }
}