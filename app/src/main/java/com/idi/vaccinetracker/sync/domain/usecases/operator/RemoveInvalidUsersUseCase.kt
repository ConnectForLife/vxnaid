package com.idi.vaccinetracker.sync.domain.usecases.operator

import com.idi.vaccinetracker.common.data.database.repositories.OperatorCredentialsRepository
import com.idi.vaccinetracker.common.domain.entities.OperatorCredentials
import com.idi.vaccinetracker.common.helpers.logInfo
import com.idi.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.idi.vaccinetracker.sync.domain.usecases.operator.base.RemoveOperatorCredentialsUseCaseBase
import javax.inject.Inject

class RemoveInvalidUsersUseCase @Inject constructor(
    private val api: VaccineTrackerSyncApiDataSource,
    override val operatorCredentialsRepository: OperatorCredentialsRepository,
) : RemoveOperatorCredentialsUseCaseBase() {

    private suspend fun List<OperatorCredentials>.deleteInactiveUsers(): List<OperatorCredentials> {
        val activeUsers = api.getActiveUsers()
        return filter { cachedOperator ->
            val isValid = activeUsers.any { it.uuid == cachedOperator.uuid }
            val isDeleted = !isValid && cachedOperator.tryDelete()
            !isDeleted
        }
    }

    suspend fun removeInvalidUsers() {
        val cachedCredentials = findOperators()
        if (cachedCredentials.isEmpty()) {
            logInfo("removeInvalidUsers: cached credentials empty")
            return
        }
        val usersLeft = cachedCredentials.deleteInactiveUsers()
        logInfo("removeInvalidUsers: ${cachedCredentials.size} -> ${usersLeft.size}")
    }
}