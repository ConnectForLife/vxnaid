package com.idi.vaccinetracker.common.data.database.models.syncerror

import com.idi.vaccinetracker.common.data.database.typealiases.DateEntity

class RoomSyncErrorOverviewModel(val metadataJson: String, val stackTrace: String, val dateCreated: DateEntity)