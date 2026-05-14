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
import com.jnj.vaccinetracker.reportsoverview.hmis105.screens.Hmis105ChildHealthFragment

@RequiresApi(Build.VERSION_CODES.Q)
class Hmis105ChildHealthFlowActivity : BaseActivity() {

    companion object {
        fun create(context: Context): Intent = Intent(context, Hmis105ChildHealthFlowActivity::class.java)
    }

    private lateinit var binding: ActivityFlowBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_flow)
        binding.lifecycleOwner = this

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, Hmis105ChildHealthFragment())
                .commit()
        }
    }
}
