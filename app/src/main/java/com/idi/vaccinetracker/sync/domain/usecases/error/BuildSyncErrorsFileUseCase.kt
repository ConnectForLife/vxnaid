package com.idi.vaccinetracker.sync.domain.usecases.error

import com.idi.vaccinetracker.common.data.database.helpers.pagingQueryList
import com.idi.vaccinetracker.common.data.database.repositories.SyncErrorRepository
import com.idi.vaccinetracker.sync.data.io.SyncErrorFileWriter
import com.idi.vaccinetracker.sync.domain.entities.BuildSyncErrorsFile
import com.idi.vaccinetracker.sync.domain.entities.SyncErrorMetadata
import com.idi.vaccinetracker.sync.domain.entities.SyncErrorState
import java.io.File
import javax.inject.Inject

class BuildSyncErrorsFileUseCase @Inject constructor(private val syncErrorRepository: SyncErrorRepository, private val syncErrorFileWriter: SyncErrorFileWriter) {

    companion object {
        private const val FIND_ALL_SYNC_ERRORS_PAGE_SIZE = 50
    }

    private suspend fun fromOne(metadata: SyncErrorMetadata): File {
        val entity = syncErrorRepository.findByKey(metadata.key)
        return syncErrorFileWriter.writeAll(listOfNotNull(entity))
    }

    private suspend fun fromAll(): File {
        val errors = pagingQueryList(0, pageSize = FIND_ALL_SYNC_ERRORS_PAGE_SIZE, queryFunction = { offset, limit ->
            syncErrorRepository.findAllByErrorStates(SyncErrorState.statesNotResolved(), offset, limit)
        })
        return syncErrorFileWriter.writeAll(errors)
    }

    suspend fun buildSyncErrorsFile(buildSyncErrorsFile: BuildSyncErrorsFile): File {
        return when (buildSyncErrorsFile) {
            BuildSyncErrorsFile.FromAll -> fromAll()
            is BuildSyncErrorsFile.FromOne -> fromOne(buildSyncErrorsFile.metadata)
        }
    }
}

