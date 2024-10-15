package com.jnj.vaccinetracker.common.helpers

import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.domain.entities.VisitDetail

fun List<VisitDetail>.findDosingVisit(): VisitDetail? {
   return findLast { visit ->
      visit.visitType == Constants.VISIT_TYPE_DOSING && hasNotOccurredYet(visit)
   }
}

private fun hasNotOccurredYet(visit: VisitDetail): Boolean {
   return visit.visitStatus != Constants.VISIT_STATUS_MISSED && visit.visitStatus != Constants.VISIT_STATUS_OCCURRED
}