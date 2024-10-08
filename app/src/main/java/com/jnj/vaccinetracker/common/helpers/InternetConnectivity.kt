package com.jnj.vaccinetracker.common.helpers

import com.jnj.vaccinetracker.config.Counters
import com.jnj.vaccinetracker.sync.data.repositories.SyncSettingsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.URI
import java.net.URISyntaxException
import java.net.UnknownHostException
import javax.inject.Inject

class InternetConnectivity @Inject constructor(
    private val dispatchers: AppCoroutineDispatchers,
    private val syncSettingsRepository: SyncSettingsRepository,
) {

    companion object {
        private val counter = Counters.InternetConnectivity
        private const val DEFAULT_INTERNET_STATE = true

        @Suppress("BlockingMethodInNonBlockingContext")
        fun hasIpAddress(url: String): Boolean {
            return try {
                val uri = URI(url)
                val host = uri.host ?: return false
                val address = InetAddress.getByName(host)
                !address.equals("")
            } catch (e: UnknownHostException) {
                // no-op
                false
            } catch (ex: URISyntaxException) {
                logError("URI Syntax Error occurred while fetching IP address for URL: $url. Exception message: ${ex.message}", ex)
                false
            } catch (ex: Exception) {
                logError("unknown error occurred while fetching ip address for $url", ex)
                false
            }
        }
    }

    private val internetConnectivityFlow = MutableStateFlow(true)

    fun observeInternetConnectivity(intervalMs: Long = counter.POLL_INTERNET_DELAY): Flow<Boolean> {
        return listOf(flow {
            while (true) {
                internetConnectivityFlow.value = fetchIsInternetConnected()
                delay(intervalMs)
            }
        }, internetConnectivityFlow).flattenMerge()
    }


    fun isInternetConnected(): Boolean = internetConnectivityFlow.value

    suspend fun isInternetConnectedAccurate(defaultConnectedState: Boolean = DEFAULT_INTERNET_STATE): Boolean {
        internetConnectivityFlow.value = fetchIsInternetConnected(defaultConnectedState)
        val state = internetConnectivityFlow.value
        logInfo("isInternetConnectedAccurate: $state (defaultConnectedState:$defaultConnectedState, url=${syncSettingsRepository.getBackendUrlOrNull()})")
        return state
    }

    private suspend fun fetchIsInternetConnected(defaultConnectedState: Boolean = DEFAULT_INTERNET_STATE): Boolean = withContext(dispatchers.io) {
        syncSettingsRepository.getBackendUrlOrNull()?.let { backendUrl ->
            hasIpAddress(backendUrl)
        } ?: defaultConnectedState
    }
}
