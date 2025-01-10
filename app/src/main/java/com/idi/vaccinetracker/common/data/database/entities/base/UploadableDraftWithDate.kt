package com.idi.vaccinetracker.common.data.database.entities.base

import com.idi.vaccinetracker.common.data.database.typealiases.DateEntity

interface UploadableDraftWithDate : UploadableDraft {
    val dateLastUploadAttempt: DateEntity?
}