package com.idi.vaccinetracker.sync.p2p.domain.usecases

import com.idi.vaccinetracker.common.data.database.ParticipantRoomDatabase
import com.idi.vaccinetracker.common.data.database.ParticipantRoomDatabaseConfig
import com.idi.vaccinetracker.common.data.repositories.EncryptionKeyRepository
import com.idi.vaccinetracker.common.exceptions.DeleteDatabaseRequiredException
import com.idi.vaccinetracker.common.helpers.logInfo
import com.idi.vaccinetracker.sync.p2p.common.util.VmpDatabaseFiles
import javax.inject.Inject

class SwapDatabasePassphraseUseCase @Inject constructor(
    private val database: ParticipantRoomDatabase,
    private val vmpDatabaseFiles: VmpDatabaseFiles,
    private val encryptionKeyRepository: EncryptionKeyRepository,
) {

    private suspend fun shouldSwap(passphrase: String): Boolean {
        val currentPassphrase = encryptionKeyRepository.getOrGenerateDatabasePassphrase()
        return currentPassphrase != passphrase
    }

    private suspend fun swapDbPassphraseImpl(passphrase: String, deleteDatabaseIfRequired: Boolean, shouldReopenDb: Boolean) {
        if (!shouldSwap(passphrase)) {
            logInfo("database passphrase is already up to date")
            return
        }
        val name = ParticipantRoomDatabaseConfig.FILE_NAME
        val dbFile = vmpDatabaseFiles.getDbFile()
        if (!dbFile.exists()) {
            // change the passphrase in the preferences
            encryptionKeyRepository.setDatabasePassphrase(passphrase)
            if (shouldReopenDb) {
                database.reopen()
            }
        } else {
            // dbFile exists
            if (deleteDatabaseIfRequired) {
                database.closeShorty {
                    val success = vmpDatabaseFiles.delete()
                    if (success) {
                        swapDbPassphraseImpl(passphrase, deleteDatabaseIfRequired = false, shouldReopenDb = false)
                    } else {
                        throw DeleteDatabaseRequiredException(dbName = name)
                    }
                }
            } else {
                throw DeleteDatabaseRequiredException(dbName = name)
            }
        }
    }

    /**
     * only swaps passphrase of database object
     */
    suspend fun swapDbPassphrase(passphrase: String, deleteDatabaseIfRequired: Boolean) {
        logInfo("swapDbPassphrase")
        swapDbPassphraseImpl(passphrase, deleteDatabaseIfRequired = deleteDatabaseIfRequired, shouldReopenDb = true)
    }
}