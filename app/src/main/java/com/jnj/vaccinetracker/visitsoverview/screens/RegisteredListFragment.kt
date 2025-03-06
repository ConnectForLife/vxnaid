package com.jnj.vaccinetracker.visitsoverview.screens

import com.jnj.vaccinetracker.common.dialogs.ReportOverviewDatePickerDialog
import com.jnj.vaccinetracker.common.ui.BaseFragment
import com.soywiz.klock.DateTime

class RegisteredListFragment(private  val registeredChildrenKey: String) : BaseFragment(),
       ReportOverviewDatePickerDialog.VisitsOverviewDatePickerListener {




    override fun onDatePicked(date: DateTime?, tag: String?) {

    }


}