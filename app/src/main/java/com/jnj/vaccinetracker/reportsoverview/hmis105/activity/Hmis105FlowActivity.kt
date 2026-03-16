package com.jnj.vaccinetracker.reportsoverview.hmis105.activity

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.ui.BaseActivity
import com.jnj.vaccinetracker.databinding.ActivityFlowBinding
import com.jnj.vaccinetracker.reportsoverview.hmis105.screens.Hmis105ReportFragment

@RequiresApi(Build.VERSION_CODES.Q)
class Hmis105FlowActivity : BaseActivity() {

    companion object {
        fun create(context: Context): Intent = Intent(context, Hmis105FlowActivity::class.java)
    }

    private lateinit var binding: ActivityFlowBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_flow)
        binding.lifecycleOwner = this

        if (savedInstanceState == null) {
            val fragment = Hmis105ReportFragment()
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, fragment)
                .commit()
        }
    }
}

