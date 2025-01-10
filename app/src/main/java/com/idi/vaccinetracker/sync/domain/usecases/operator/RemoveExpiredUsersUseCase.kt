package com.idi.vaccinetracker.sync.domain.usecases.operator

import com.idi.vaccinetracker.common.data.database.repositories.OperatorCredentialsRepository
import com.idi.vaccinetracker.common.data.database.typealiases.dateNow
import com.idi.vaccinetracker.common.domain.entities.OperatorCredentials
import com.idi.vaccinetracker.common.domain.usecases.masterdata.GetConfigurationUseCase
import com.idi.vaccinetracker.common.helpers.logInfo
import com.idi.vaccinetracker.sync.domain.usecases.operator.base.RemoveOperatorCredentialsUseCaseBase
import javax.inject.Inject

class RemoveExpiredUsersUseCase @Inject constructor(
    override val operatorCredentialsRepository: OperatorCredentialsRepository,
    private val getConfigurationUseCase: GetConfigurationUseCase,
) : RemoveOperatorCredentialsUseCaseBase() {

    private suspend fun List<OperatorCredentials>.deleteExpiredUsers(): List<OperatorCredentials> {
        val config = getConfigurationUseCase.getMasterData()
        val retentionTime = config.operatorCredentialsRetentionTime
        return if (retentionTime > 0) {
            val dateNow = dateNow()
            fun OperatorCredentials.isExpired() = dateCreated.time + retentionTime < dateNow.time
            filter { cachedOperator ->
                val isDeleted = cachedOperator.isExpired() && cachedOperator.tryDelete()
                !isDeleted
            }
        } else {
            this
        }
    }

    suspend fun removeExpiredUsers() {
        val cachedCredentials = findOperators()
        if (cachedCredentials.isEmpty()) {
            logInfo("removeExpiredUsers: cached credentials empty")
            return
        }
        val usersLeft = cachedCredentials.deleteExpiredUsers()
        logInfo("removeExpiredUsers: ${cachedCredentials.size} -> ${usersLeft.size}")
    }
}