package com.jnj.vaccinetracker.common.domain.usecases

import com.jnj.vaccinetracker.common.data.database.repositories.DraftParticipantRepository
import com.jnj.vaccinetracker.common.data.database.transaction.ParticipantDbTransactionRunner
import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity
import com.jnj.vaccinetracker.common.data.database.typealiases.dateNow
import com.jnj.vaccinetracker.common.data.files.ParticipantDataFileIO
import com.jnj.vaccinetracker.common.domain.entities.*
import com.jnj.vaccinetracker.common.exceptions.*
import com.jnj.vaccinetracker.sync.domain.helpers.SyncLogger
import com.jnj.vaccinetracker.sync.domain.usecases.upload.UploadUpdateDraftParticipantUseCase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateParticipantUseCase @Inject constructor(
    private val participantDataFileIO: ParticipantDataFileIO,
    private val draftParticipantRepository: DraftParticipantRepository,
    private val uploadUpdateDraftParticipantUseCase: UploadUpdateDraftParticipantUseCase,
    private val findParticipantByParticipantIdUseCase: FindParticipantByParticipantIdUseCase,
    private val transactionRunner: ParticipantDbTransactionRunner,
    private val syncLogger: SyncLogger,
) {

    private suspend fun ImageBytes.writeToDisk(participantImageFile: DraftParticipantImageFile) {
        participantDataFileIO.writeParticipantDataFile(participantImageFile, bytes, overwrite = false)
    }

    private fun UpdateParticipant.toDomain(
        participantUuid: String,
        registrationDate: DateEntity,
        participantBiometricsTemplateFile: DraftParticipantBiometricsTemplateFile?,
        participantImageFile: DraftParticipantImageFile?,
    ) = DraftParticipant(
        participantUuid = participantUuid,
        registrationDate = registrationDate,
        image = participantImageFile,
        biometricsTemplate = participantBiometricsTemplateFile,
        participantId = participantId,
        nin = nin,
        childNumber = childNumber,
        gender = gender,
        isBirthDateEstimated = isBirthDateEstimated,
        birthDate = birthDate,
        attributes = attributes,
        address = address,
        draftState = DraftState.initialState(),
        isUpdate = true,
    )

    private suspend fun writeImageToDisk(file: DraftParticipantImageFile, imageBytes: ImageBytes) {
        imageBytes.writeToDisk(file)
    }

    suspend fun updateParticipant(updateParticipant: UpdateParticipant): DraftParticipant {
        findParticipantByParticipantIdUseCase.findByParticipantId(updateParticipant.participantId)
            ?: throw ParticipantNotFoundException()

        val deletedParticipant = findParticipantByParticipantIdUseCase.findDeletedParticipantbyId(updateParticipant.participantId)
        if (deletedParticipant != null) {
            throw ParticipantDeletedException()
        }

        val updateDate = dateNow()
        val participantUuid = updateParticipant.participantUuid
        val imageFile = updateParticipant.image?.let { DraftParticipantImageFile.newFile(participantUuid) }
        var success = false

        return try {
            imageFile?.let { writeImageToDisk(it, updateParticipant.image) }

            val participant = updateParticipant.toDomain(
                participantUuid = participantUuid,
                registrationDate = updateDate,
                participantBiometricsTemplateFile = null, // No biometrics file for update
                participantImageFile = imageFile
            )

            transactionRunner.withTransaction {
                draftParticipantRepository.insert(participant, orReplace = true)
            }

            success = true
            participant
        } finally {
            if (!success) {
                imageFile?.let { participantDataFileIO.deleteParticipantDataFile(it) }
            }
        }
    }

}