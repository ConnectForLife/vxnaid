package com.idi.vaccinetracker.sync.p2p.data.encryption

import com.idi.vaccinetracker.common.data.helpers.Base64
import com.idi.vaccinetracker.common.data.repositories.EncryptionKeyRepository
import com.idi.vaccinetracker.sync.data.repositories.SyncUserCredentialsRepository
import com.idi.vaccinetracker.sync.p2p.common.models.dtos.EncryptedSecret
import javax.inject.Inject

class DataFileSecretEncryptionService @Inject constructor(
    private val syncUserCredentialsRepository: SyncUserCredentialsRepository,
    private val peerPassphraseEncryptionService: PeerPassphraseEncryptionService,
    private val encryptionKeyRepository: EncryptionKeyRepository,
    private val base64: Base64,
) {

    suspend fun encryptDataFileSecret(osApiLevel: Int): EncryptedSecret {
        return peerPassphraseEncryptionService.encrypt(
            syncUserCredentialsRepository.getSyncUserCredentials(),
            osApiLevel,
            encryptionKeyRepository.getOrGenerateAesEncryptionSecretKey().let { base64.encode(it) }
        )
    }

    suspend fun decryptDataFileSecret(dataFileSecret: EncryptedSecret, osApiLevel: Int): ByteArray {
        val secret = peerPassphraseEncryptionService.decrypt(
            syncUserCredentialsRepository.getSyncUserCredentials(),
            osApiLevel,
            dataFileSecret
        )
        return base64.decode(secret)
    }
}