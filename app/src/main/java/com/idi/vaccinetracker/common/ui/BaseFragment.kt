package com.idi.vaccinetracker.common.ui

import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import com.idi.vaccinetracker.common.di.ResourcesWrapper
import com.idi.vaccinetracker.common.ui.dialog.ValidationErrorDialog
import dagger.android.support.DaggerFragment
import javax.inject.Inject

/**
 * @author maartenvangiel
 * @version 1
 */
abstract class BaseFragment : DaggerFragment(), ResourcesWrapper, MvvmView, UiFlowExt {
    private companion object {
        private const val TAG_VALIDATION_ERROR_DIALOG = "validationErrorDialog"
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    protected inline val resourcesWrapper: ResourcesWrapper
        get() = this

    override fun getInt(resId: Int): Int {
        return resources.getInteger(resId)
    }

    override fun getColor(resId: Int): Int {
        return ContextCompat.getColor(requireContext(), resId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startObservingWhenStarted()
    }

    private fun startObservingWhenStarted() {
        viewLifecycleOwnerLiveData.observe(
            this, { lifecycleOwner ->
                if (lifecycleOwner != null)
                    observeViewModel(lifecycleOwner)
            }
        )
    }

    override fun observeViewModel(lifecycleOwner: LifecycleOwner) {
        //make this function optional
    }

    fun displayValidationErrorDialog(errorList: List<String>) {
        ValidationErrorDialog.create(errorList).show(childFragmentManager, TAG_VALIDATION_ERROR_DIALOG)
    }
}
