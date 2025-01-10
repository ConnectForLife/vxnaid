package com.idi.vaccinetracker.common.helpers

import android.content.res.Resources

val Int.dpToPx: Int
   get() = (this * Resources.getSystem().displayMetrics.density).toInt()