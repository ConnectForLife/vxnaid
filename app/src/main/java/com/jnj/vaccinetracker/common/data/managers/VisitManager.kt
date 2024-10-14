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
        visitLocation: String? = null
    ) {
        val locationUuid = syncSettingsRepository.getSiteUuid()
            ?: throw NoSiteUuidAvailableException("Trying to register dosing visit without a selected site")

        val operatorUuid = userRepository.getUser()?.uuid
            ?: throw OperatorUuidNotAvailableException("Trying to register dosing visit without stored operator UUID")

        val attributes = buildVisitAttributes(operatorUuid, dosingNumber, visitLocation)

        val observations = buildObservations(
            substanceObservations = substanceObservations,
            otherSubstanceObservations = otherSubstanceObservations,
            encounterDatetime = encounterDatetime
        )

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

    private fun buildVisitAttributes(operatorUuid: String, dosingNumber: Int, visitLocation: String?): Map<String, String> {
        return mapOf(
            Constants.ATTRIBUTE_VISIT_STATUS to Constants.VISIT_STATUS_OCCURRED,
            Constants.ATTRIBUTE_OPERATOR to operatorUuid,
            Constants.ATTRIBUTE_VISIT_DOSE_NUMBER to dosingNumber.toString(),
            *listOfNotNull(visitLocation?.let { Constants.ATTRIBUTE_VISIT_LOCATION to visitLocation }).toTypedArray()
        )
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

    suspend fun updateVisitAttributes(visit: VisitDetail?, participantUuid: String, visitAttributes: Map<String, String>) {
        val locationUuid = syncSettingsRepository.getSiteUuid()
            ?: throw NoSiteUuidAvailableException("Trying to register dosing visit without a selected site")

        val request = visit?.let{
            val appendedVisitAttributes = it.attributes + visitAttributes
            val parsedObservations: Map<String, String> = it.observations.mapValues { entry -> entry.value.value }
            UpdateVisit(
                visitUuid = it.uuid,
                participantUuid = participantUuid,
                startDatetime = it.startDate,
                locationUuid = locationUuid,
                observations = parsedObservations,
                attributes = appendedVisitAttributes
            )
        } ?: throw VisitNotFound()
        updateVisitUseCase.updateVisitAndReplace(request)
    }

    suspend fun updateVisitObservations(visit: VisitDetail?, participantUuid: String, visitObservations: Map<String, String>) {
        val locationUuid = syncSettingsRepository.getSiteUuid()
            ?: throw NoSiteUuidAvailableException("Trying to register dosing visit without a selected site")

        val request = visit?.let{
            val parsedObservations: Map<String, String> = it.observations.mapValues { entry -> entry.value.value }
            val appendedObservations = parsedObservations + visitObservations
            UpdateVisit(
                visitUuid = it.uuid,
                participantUuid = participantUuid,
                startDatetime = it.startDate,
                locationUuid = locationUuid,
                observations = appendedObservations,
                attributes = it.attributes
            )
        } ?: throw VisitNotFound()
        updateVisitUseCase.updateVisitAndReplace(request)
    }
}