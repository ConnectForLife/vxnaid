package com.idi.vaccinetracker.sync.domain.usecases.masterdata

import com.idi.vaccinetracker.common.data.repositories.MasterDataRepository
import com.idi.vaccinetracker.common.domain.entities.MasterDataFile
import com.idi.vaccinetracker.common.helpers.logError
import com.idi.vaccinetracker.common.helpers.rethrowIfFatal
import kotlinx.coroutines.yield
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetMasterDataHashUseCase @Inject constructor(
    private val masterDataRepository: MasterDataRepository,
) {
    suspend fun getMasterDataHash(masterDataFile: MasterDataFile): String? {
        return try {
            masterDataRepository.md5Hash(masterDataFile)
        } catch (ex: Exception) {
            yield()
            ex.rethrowIfFatal()
            logError("failed to calc md5 hash for file: $masterDataFile", ex)
            // if we can't calculate to md5 hash then pretend the file doesn't exist
            null
        }
    }
}