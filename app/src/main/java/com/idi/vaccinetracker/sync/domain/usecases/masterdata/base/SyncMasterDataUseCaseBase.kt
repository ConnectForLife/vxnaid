package com.idi.vaccinetracker.sync.domain.usecases.masterdata.base

import com.idi.vaccinetracker.common.data.repositories.MasterDataRepository
import com.idi.vaccinetracker.common.domain.entities.MasterDataFile
import com.idi.vaccinetracker.common.exceptions.GetMasterDataRemoteException
import com.idi.vaccinetracker.common.exceptions.StoreMasterDataException
import com.idi.vaccinetracker.common.helpers.logInfo
import com.idi.vaccinetracker.sync.data.models.SyncDate
import com.idi.vaccinetracker.sync.domain.entities.SyncErrorMetadata
import com.idi.vaccinetracker.sync.domain.helpers.SyncLogger

interface SyncMasterDataUseCase {
    suspend fun sync(dateModified: SyncDate)
}

abstract class SyncMasterDataUseCaseBase<T> : SyncMasterDataUseCase {

    protected abstract suspend fun getMasterDataRemote(): T
    protected abstract suspend fun storeMasterData(masterData: T)

    protected abstract val syncLogger: SyncLogger
    protected abstract val masterDataRepository: MasterDataRepository
    protected abstract val masterDataFile: MasterDataFile

    private suspend fun getMasterDataRemoteOrThrow(): T {
        val syncErrorMetadata = SyncErrorMetadata.MasterData(masterDataFile, SyncErrorMetadata.MasterData.Action.GET_MASTER_DATA_CALL)
        return try {
            getMasterDataRemote().also {
                syncLogger.clearSyncError(syncErrorMetadata)
            }
        } catch (throwable: Throwable) {
            syncLogger.logSyncError(syncErrorMetadata, throwable)
            throw GetMasterDataRemoteException("getMasterDataRemote error $masterDataFile", throwable)
        }
    }

    private suspend fun storeMasterDataOrThrow(masterData: T, dateModified: SyncDate) {
        val syncErrorMetadata = SyncErrorMetadata.MasterData(masterDataFile, SyncErrorMetadata.MasterData.Action.PERSIST_MASTER_DATA)
        try {
            storeMasterData(masterData)
            masterDataRepository.storeDateModifiedOrThrow(masterDataFile, dateModified).also {
                syncLogger.clearSyncError(syncErrorMetadata)
            }
        } catch (throwable: Throwable) {
            syncLogger.logSyncError(syncErrorMetadata, throwable)
            throw StoreMasterDataException("storeMasterData error $masterDataFile", throwable)
        }
    }


    /**
     * first [getMasterDataRemote] then [storeMasterData] and attach [dateModified] as file metadata
     */
    override suspend fun sync(dateModified: SyncDate) {
        logInfo("sync $this")
        val masterData = getMasterDataRemoteOrThrow()
        storeMasterDataOrThrow(masterData, dateModified)
    }
}