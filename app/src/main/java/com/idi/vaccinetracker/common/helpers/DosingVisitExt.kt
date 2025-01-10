package com.idi.vaccinetracker.common.helpers

import com.idi.vaccinetracker.common.data.models.Constants
import com.idi.vaccinetracker.common.domain.entities.VisitDetail

fun List<VisitDetail>.findDosingVisit(): VisitDetail? {
   return findLast { visit ->
      visit.visitType == Constants.VISIT_TYPE_DOSING && hasNotOccurredYet(visit)
   }
}

private fun hasNotOccurredYet(visit: VisitDetail): Boolean {
   return visit.visitStatus != Constants.VISIT_STATUS_MISSED && visit.visitStatus != Constants.VISIT_STATUS_OCCURRED
}