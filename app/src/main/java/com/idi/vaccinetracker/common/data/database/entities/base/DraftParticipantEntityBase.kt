package com.idi.vaccinetracker.common.data.database.entities.base

import com.idi.vaccinetracker.common.data.database.typealiases.DateEntity

interface DraftParticipantEntityBase : ParticipantEntityBase, UploadableDraft {
    val registrationDate: DateEntity
    val isUpdate: Boolean?
}