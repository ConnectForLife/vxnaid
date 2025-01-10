package com.idi.vaccinetracker.sync.domain.services

import com.idi.vaccinetracker.common.data.database.typealiases.dateNow
import com.idi.vaccinetracker.common.domain.entities.DateModifiedOccurrence
import com.idi.vaccinetracker.common.domain.entities.SyncEntityType
import com.idi.vaccinetracker.common.domain.usecases.FindMostRecentDateModifiedOccurrenceUseCase
import com.idi.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.idi.vaccinetracker.common.helpers.NetworkConnectivity
import com.idi.vaccinetracker.common.helpers.uuid
import com.idi.vaccinetracker.sync.data.helpers.ServerPollUtil
import com.idi.vaccinetracker.sync.data.mappers.SyncScopeToDtoMapper
import com.idi.vaccinetracker.sync.data.models.SyncScopeLevel
import com.idi.vaccinetracker.sync.data.models.SyncStatus
import com.idi.vaccinetracker.sync.domain.entities.SyncScope
import com.idi.vaccinetracker.sync.domain.factories.DownloadSyncRecordsUseCaseFactory
import com.idi.vaccinetracker.sync.domain.helpers.ForceSyncObserver
import com.idi.vaccinetracker.sync.domain.helpers.SyncLogger
import com.idi.vaccinetracker.sync.domain.helpers.SyncSettingsObserver
import com.idi.vaccinetracker.sync.domain.usecases.download.base.DownloadSyncRecordsUseCase
import com.idi.vaccinetracker.sync.domain.usecases.failed.DownloadFailedSyncRecordsUseCase
import com.idi.vaccinetracker.sync.domain.usecases.failed.FindAllFailedSyncRecordsByDateLastDownloadAttemptUseCase
import com.idi.vaccinetracker.sync.domain.usecases.syncscope.GetSyncScopeUseCase
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotHaveSize
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class ParticipantDataDownstreamSyncServiceTest : FunSpec({
    val dispatcher = TestCoroutineDispatcher()
    val networkConnectivity: NetworkConnectivity = mockk(relaxUnitFun = true)
    val forceSyncObserver: ForceSyncObserver = mockk()
    val dispatchers = AppCoroutineDispatchers.fromSingleDispatcher(dispatcher)
    val findMostRecentDateModifiedOccurrence: FindMostRecentDateModifiedOccurrenceUseCase = mockk()
    val getSyncScopeUseCase: GetSyncScopeUseCase = mockk()
    val downloadSyncRecordsUseCaseFactory: DownloadSyncRecordsUseCaseFactory = mockk()
    val syncSettingsObserver: SyncSettingsObserver = mockk()
    val serverPollUtil = ServerPollUtil(syncSettingsObserver, networkConnectivity, forceSyncObserver, dispatchers)
    val syncLogger: SyncLogger = mockk(relaxUnitFun = true)
    val syncScopeDtoMapper: SyncScopeToDtoMapper = mockk()
    val downloadFailedSyncRecordsUseCase: DownloadFailedSyncRecordsUseCase = mockk(relaxUnitFun = true)
    val findAllFailedSyncRecordsByDateLastDownloadAttemptUseCase: FindAllFailedSyncRecordsByDateLastDownloadAttemptUseCase = mockk()
    coEvery { findAllFailedSyncRecordsByDateLastDownloadAttemptUseCase.findAllByDateLastDownloadAttemptLesserThan(any(), any(), any()) } returns emptyList()
    val sut = ParticipantDataDownstreamSyncService(
        dispatchers,
        networkConnectivity,
        findMostRecentDateModifiedOccurrence,
        getSyncScopeUseCase,
        downloadSyncRecordsUseCaseFactory, syncSettingsObserver, serverPollUtil, syncLogger, downloadFailedSyncRecordsUseCase,
        findAllFailedSyncRecordsByDateLastDownloadAttemptUseCase,
        syncScopeDtoMapper
    )

    data class CounterLog(val syncEntityType: SyncEntityType, val counter: Int)

    val counters = mutableListOf<CounterLog>()
    val mutex = Mutex()
    val downloadCount = 10
    suspend fun logDownload(log: CounterLog) = mutex.withLock {
        counters += log
    }

    fun createDownloader(syncEntityType: SyncEntityType): DownloadSyncRecordsUseCase {
        val useCase: DownloadSyncRecordsUseCase = mockk()
        var counter = downloadCount
        coEvery { useCase.download(any()) } coAnswers {
            withContext(Dispatchers.IO) {
                if (counter > 0) {
                    println("$syncEntityType download $counter")
                    delay(25)
                    counter--
                    logDownload(CounterLog(syncEntityType, counter))
                    SyncStatus.OUT_OF_SYNC
                } else
                    SyncStatus.OK
            }
        }
        return useCase
    }

    test("when out of sync, fetch records for all sync call in a concurrent fashion so not sequential") {
        // Arrange
        SyncEntityType.values().forEach { syncEntityType ->
            every { downloadSyncRecordsUseCaseFactory.create(syncEntityType) } returns createDownloader(syncEntityType)
            coEvery { findMostRecentDateModifiedOccurrence.findMostRecentDateModifiedOccurrence(syncEntityType) } returns DateModifiedOccurrence(dateNow(), listOf("uuid"))
        }
        coEvery { getSyncScopeUseCase.getSyncScope() } returns SyncScope(country = "India",
            siteUuid = uuid(),
            cluster = null,
            level = SyncScopeLevel.COUNTRY,
            dateCreated = dateNow())
        coEvery { networkConnectivity.isConnectedFast() } returns true
        coEvery { forceSyncObserver.observeForceSync() } returns emptyFlow()
        coEvery { syncSettingsObserver.observeSiteSelectionChanges() } returns emptyFlow()
        coEvery { syncSettingsObserver.awaitSyncSettingsAvailable(any()) } returns Unit
        coEvery { syncSettingsObserver.isSyncSettingsAvailable() } returns true
        coEvery { syncSettingsObserver.isSiteSelectionAvailable() } returns true
        coEvery { syncSettingsObserver.observeSyncSettingsChanges() } returns flowOf(Unit)
        // Act
        sut.start()
        while (counters.size < 35) {
            delay(100)
        }
        // Assert
        val first10Downloads = counters.take(downloadCount)
        first10Downloads shouldHaveSize downloadCount
        val firstSyncEntityType = SyncEntityType.values().first()
        first10Downloads.filter { it.syncEntityType == firstSyncEntityType } shouldNotHaveSize downloadCount
    }
})
