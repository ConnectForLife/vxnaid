package com.jnj.vaccinetracker.login

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.helpers.hideKeyboard
import com.jnj.vaccinetracker.common.ui.BaseActivity
import com.jnj.vaccinetracker.common.ui.SyncBanner
import com.jnj.vaccinetracker.databinding.ActivityLoginBinding
import com.jnj.vaccinetracker.participantflow.ParticipantFlowActivity
import com.jnj.vaccinetracker.settings.SettingsDialog
import com.jnj.vaccinetracker.update.UpdateDialog
import kotlinx.coroutines.flow.onEach

/**
 * @author maartenvangiel
 * @version 1
 */
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
    private var selectedVisitPlace: String? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        viewModel.init(true)

        binding.btnLogin.setOnClickListener { login() }
        binding.editPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                login()
                true
            } else {
                false
            }
        }

        val textInputPasswordLayout = findViewById<TextInputLayout>(R.id.textInputPassword)
        val editPassword = findViewById<TextInputEditText>(R.id.edit_password)
        val textInputOutreachName = findViewById<TextInputLayout>(R.id.textInputOutreachName)
        val visitPlaceIcon = findViewById<ImageView>(R.id.img_location_name)
        textInputOutreachName.visibility = View.GONE
        visitPlaceIcon.visibility = View.GONE

        textInputPasswordLayout.setEndIconOnClickListener {
            if (editPassword.inputType == (android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                editPassword.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                editPassword.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            }

            editPassword.setSelection(editPassword.text?.length ?: 0)
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
            selectedVisitPlace = visitPlaces[position]
            Log.e("Selected Visit Place", "Selected Visit Place: $selectedVisitPlace")
            if (selectedVisitPlace == Constants.VISIT_PLACE_OUTREACH) {
                textInputOutreachName.visibility = View.VISIBLE
                visitPlaceIcon.visibility = View.VISIBLE
            } else {
                textInputOutreachName.visibility = View.GONE
                visitPlaceIcon.visibility = View.GONE
            }
        }

        binding.root.setOnClickListener { hideKeyboard() }
        binding.btnUpdate.setOnClickListener { showUpdateDialog() }
        observeViewModel(this)
    }

    override val syncBanner: SyncBanner
        get() = binding.syncBanner

    override val isAuthenticatedOperatorScreen: Boolean
        get() = false

    @RequiresApi(Build.VERSION_CODES.O)
    private fun observeViewModel(lifecycleOwner: LifecycleOwner) {
        viewModel.loginCompleted
            .asFlow()
            .onEach {
                onLoginCompleted()
            }
            .launchIn(lifecycleOwner)

        viewModel.prefillUsername.observe(lifecycleOwner) { prefillUsername ->
            if (binding.editUsername.text.isEmpty()) {
                binding.editUsername.setText(prefillUsername)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_settings, menu)
        loginActivityMenuHelper.onCreateOptionsMenu(menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (loginActivityMenuHelper.onOptionsItemSelected(item))
            return true
        return when (item.itemId) {
            R.id.action_settings -> {
                showSettingsDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showSettingsDialog() {
        SettingsDialog().show(supportFragmentManager, TAG_SETTINGS_DIALOG)
    }

    private fun showUpdateDialog() {
        UpdateDialog().show(supportFragmentManager, TAG_UPDATE_DIALOG)
    }

    private fun login() {
        val username = binding.editUsername.text.toString()
        val password = binding.editPassword.text.toString()
        val visitPlace = binding.dropdownLoginVisitPlace.text.toString()
        val outreachName = binding.editOutreachName.text.toString().uppercase()
        if((selectedVisitPlace == Constants.VISIT_PLACE_OUTREACH) && (outreachName.isEmpty())){
            binding.editOutreachName.error = resourcesWrapper.getString(R.string.login_label_validation_no_outreach_name)
        } else {
            val defaultOutreachName = outreachName.ifEmpty { "No Outreach" }
            saveVisitPlaceToMemory(visitPlace, defaultOutreachName)
            viewModel.login(username, password, visitPlace)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun onLoginCompleted() {
        startActivity(ParticipantFlowActivity.create(this))
        finish()
    }

    private fun saveVisitPlaceToMemory(visitPlace: String, outreachName: String) {
        val sharedPreferences = getSharedPreferences(Constants.USER_PREFERENCES_FILE_NAME, MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(Constants.VISIT_PLACE_FILE_KEY, visitPlace)
        editor.putString(Constants.OUTREACH_NAME, outreachName)
        editor.apply()
    }
}