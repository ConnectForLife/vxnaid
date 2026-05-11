package com.jnj.vaccinetracker.common.data.managers

import com.jnj.vaccinetracker.common.data.database.typealiases.dateNow
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.data.repositories.UserRepository
import com.jnj.vaccinetracker.common.domain.entities.CreateVisit
import com.jnj.vaccinetracker.common.domain.entities.UpdateVisit
import com.jnj.vaccinetracker.common.domain.entities.VisitDetail
import com.jnj.vaccinetracker.common.domain.usecases.CreateVisitUseCase
import com.jnj.vaccinetracker.common.domain.usecases.GetParticipantVisitDetailsUseCase
import com.jnj.vaccinetracker.common.domain.usecases.GetUpcomingVisitUseCase
import com.jnj.vaccinetracker.common.domain.usecases.UpdateVisitUseCase
import com.jnj.vaccinetracker.common.exceptions.NoSiteUuidAvailableException
import com.jnj.vaccinetracker.common.exceptions.OperatorUuidNotAvailableException
import com.jnj.vaccinetracker.common.exceptions.VisitNotFound
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.common.util.DateUtil
import com.jnj.vaccinetracker.sync.data.repositories.SyncSettingsRepository
import com.jnj.vaccinetracker.sync.domain.entities.UpcomingVisit
import com.soywiz.klock.DateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author maartenvangiel
 * @version 1
 */
@Singleton
class VisitManager @Inject constructor(
    private val userRepository: UserRepository,
    private val syncSettingsRepository: SyncSettingsRepository,
    private val getParticipantVisitDetailsUseCase: GetParticipantVisitDetailsUseCase,
    private val updateVisitUseCase: UpdateVisitUseCase,
    private val getUpcomingVisitUseCase: GetUpcomingVisitUseCase,
) {

    suspend fun getVisitsForParticipant(participantUuid: String): List<VisitDetail> = getParticipantVisitDetailsUseCase.getParticipantVisitDetails(participantUuid)

    suspend fun registerDosingVisit(
        participantUuid: String,
        encounterDatetime: Date,
        visitUuid: String,
        dosingNumber: Int,
        substanceObservations: Map<String, Map<String, String>>? = null,
        otherSubstanceObservations: Map<String, String>? = null,
        visitLocation: String? = null,
        visitOutreachName: String? = null,
        visitTypeVxnaid: String? = null,
        referralObservations: Map<String, String> = emptyMap(),
        attachedClinic: String? = null,
    ) {
        val locationUuid = syncSettingsRepository.getSiteUuid()
            ?: throw NoSiteUuidAvailableException("Trying to register dosing visit without a selected site")

        val operatorUuid = userRepository.getUser()?.uuid
            ?: throw OperatorUuidNotAvailableException("Trying to register dosing visit without stored operator UUID")

        val attributes = buildVisitAttributes(operatorUuid, dosingNumber, visitLocation, visitOutreachName, visitTypeVxnaid, locationUuid, attachedClinic)

        var observations = buildObservations(
            substanceObservations = substanceObservations,
            otherSubstanceObservations = otherSubstanceObservations,
            encounterDatetime = encounterDatetime
        )

        if (referralObservations.isNotEmpty()) {
            observations = observations + referralObservations
        }

        val request = UpdateVisit(
            visitUuid = visitUuid,
            startDatetime = encounterDatetime,
            participantUuid = participantUuid,
            locationUuid = locationUuid,
            attributes = attributes,
            observations = observations
        )

        updateVisitUseCase.updateVisit(request)
    }

    private fun buildVisitAttributes(operatorUuid: String, dosingNumber: Int, visitLocation: String?, visitOutreachName: String?, visitTypeVxnaid: String?, locationUuid: String, attachedClinic: String?): Map<String, String> {
        val attributes = mutableMapOf(
            Constants.ATTRIBUTE_VISIT_STATUS to Constants.VISIT_STATUS_OCCURRED,
            Constants.ATTRIBUTE_OPERATOR to operatorUuid,
            Constants.ATTRIBUTE_VISIT_DOSE_NUMBER to dosingNumber.toString(),
            Constants.ATTRIBUTE_VISIT_TYPE_VXNAID to visitTypeVxnaid.toString()
        )
        if (visitLocation != null) {
            attributes[Constants.ATTRIBUTE_VISIT_LOCATION] = visitLocation
        }
        if (visitOutreachName != null) {
            attributes[Constants.ATTRIBUTE_VISIT_OUTREACH_NAME] = visitOutreachName
        }
        if (attachedClinic != null) {
            attributes[Constants.ATTRIBUTE_VISIT_ATTACHED_CLINIC] = attachedClinic
        }
        return attributes
    }

    private fun buildObservations(
        substanceObservations: Map<String, Map<String, String>>?,
        otherSubstanceObservations: Map<String, String>?,
        encounterDatetime: Date
    ): Map<String, String> {
        return mutableMapOf<String, String>().apply {
            // convention for obs for a vaccine is its conceptName plus Date/Barcode/Manufacturer ex: Polio 0 Barcode
            substanceObservations?.forEach { (conceptName, obsMap) ->
                if (obsMap[Constants.DATE_STR].isNullOrEmpty()) {
                    // Date needs to be always added if not exists yet
                    put("$conceptName ${Constants.DATE_STR}", DateUtil.convertDateToString(encounterDatetime, DateFormat.FORMAT_DATE.toString()))
                }

                obsMap.forEach { (key, value) ->
                    val fullKey = "$conceptName $key"
                    put(fullKey, value)
                }
            }

            otherSubstanceObservations?.forEach { (conceptName, conceptValue) ->
                put(conceptName, conceptValue)
            }
        }
    }

    suspend fun getUpcomingVisit(participantUuid: String): UpcomingVisit? = getUpcomingVisitUseCase.getUpcomingVisit(participantUuid, date = dateNow())

    suspend fun updateVisitAttributes(
        visit: VisitDetail?,
        participantUuid: String,
        visitAttributes: Map<String, String>,
        referralObservations: Map<String, String> = emptyMap()
    ) {
        val locationUuid = syncSettingsRepository.getSiteUuid()
            ?: throw NoSiteUuidAvailableException("Trying to register dosing visit without a selected site")

        val request = visit?.let{
            val appendedVisitAttributes = it.attributes + visitAttributes
            val parsedObservations: Map<String, String> = it.observations.mapValues { entry -> entry.value.value }
            val allObservations = parsedObservations + referralObservations
            UpdateVisit(
                visitUuid = it.uuid,
                participantUuid = participantUuid,
                startDatetime = it.startDate,
                locationUuid = locationUuid,
                observations = allObservations,
                attributes = appendedVisitAttributes
            )
        } ?: throw VisitNotFound()
        updateVisitUseCase.updateVisitAndReplace(request)
    }

    suspend fun updateVisitObservations(visit: VisitDetail?, participantUuid: String, visitObservations: Map<String, String>, appendObservations: Boolean = true) {
        val locationUuid = syncSettingsRepository.getSiteUuid()
            ?: throw NoSiteUuidAvailableException("Trying to register dosing visit without a selected site")

        val request = visit?.let {
            val parsedObservations: Map<String, String> = it.observations.mapValues { entry -> entry.value.value }
            val newObservations = if (appendObservations) {
                parsedObservations + visitObservations
            } else {
                visitObservations
            }
            UpdateVisit(
                visitUuid = it.uuid,
                participantUuid = participantUuid,
                startDatetime = it.startDate,
                locationUuid = locationUuid,
                observations = newObservations,
                attributes = it.attributes
            )
        } ?: throw VisitNotFound()
        updateVisitUseCase.updateVisitAndReplace(request)
    }
}