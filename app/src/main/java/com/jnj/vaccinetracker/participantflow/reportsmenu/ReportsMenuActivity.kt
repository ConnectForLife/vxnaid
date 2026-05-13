package com.jnj.vaccinetracker.participantflow.reportsmenu

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.ui.BaseActivity
import com.jnj.vaccinetracker.reportsoverview.childrenoverview.activity.ReportsOverviewFlowActivity
import com.jnj.vaccinetracker.visitsoverview.activity.VisitsOverviewFlowActivity

class ReportsMenuActivity : BaseActivity() {

    companion object {
        fun create(context: Context): Intent {
            return Intent(context, ReportsMenuActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_reports_menu)
        title = getString(R.string.reports_label)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val btnVisits = findViewById<Button>(R.id.btn_visits_overview)
        val btnReports = findViewById<Button>(R.id.btn_reports_overview)

        btnVisits.setOnClickListener {
            startActivity(VisitsOverviewFlowActivity.create(this))
            setForwardAnimation()
        }

        btnReports.setOnClickListener {
            startActivity(ReportsOverviewFlowActivity.create(this))
            setForwardAnimation()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

