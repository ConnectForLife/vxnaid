package com.idi.vaccinetracker.sync.domain.usecases.syncscope.base

import com.idi.vaccinetracker.common.data.database.helpers.pagingQuery
import com.idi.vaccinetracker.common.data.files.ParticipantDataFileIO
import com.idi.vaccinetracker.common.domain.entities.ParticipantDataFile
import com.idi.vaccinetracker.common.helpers.logError
import com.idi.vaccinetracker.common.helpers.rethrowIfFatal
import kotlinx.coroutines.yield

abstract class DeleteDataFileUseCaseBase {
    companion object {
        private const val PAGE_SIZE = 5_000
    }

    protected abstract val participantDataFileIO: ParticipantDataFileIO
    protected suspend fun <T : ParticipantDataFile> deleteFilesQuery(queryFunction: suspend (offset: Int, limit: Int) -> List<T>) {
        pagingQuery(pageSize = PAGE_SIZE,
            queryFunction = queryFunction) { files -> files.deleteFiles() }
    }

    private suspend fun List<ParticipantDataFile>.deleteFiles() {
        forEach { file ->
            try {
                participantDataFileIO.deleteParticipantDataFile(file)
            } catch (ex: Exception) {
                yield()
                ex.rethrowIfFatal()
                logError("can't delete file: $file", ex)
            }
        }
    }
}