package com.jnj.vaccinetracker.common.data.database.migrations

import androidx.room.DeleteTable
import androidx.room.migration.AutoMigrationSpec

@DeleteTable.Entries(
    DeleteTable(tableName = "draft_child_health_plus_service"),
    DeleteTable(tableName = "draft_child_health_plus")
)
class ParticipantAutoMigrationSpec16to17 : AutoMigrationSpec