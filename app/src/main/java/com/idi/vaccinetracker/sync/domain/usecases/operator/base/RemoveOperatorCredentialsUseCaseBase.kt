package com.idi.vaccinetracker.sync.domain.usecases.operator.base

import com.idi.vaccinetracker.common.data.database.repositories.OperatorCredentialsRepository
import com.idi.vaccinetracker.common.domain.entities.OperatorCredentials
import com.idi.vaccinetracker.common.helpers.logError
import com.idi.vaccinetracker.common.helpers.rethrowIfFatal
import kotlinx.coroutines.yield

abstract class RemoveOperatorCredentialsUseCaseBase {

    protected abstract val operatorCredentialsRepository: OperatorCredentialsRepository


    protected suspend fun findOperators() = operatorCredentialsRepository.findAll()

    protected suspend fun OperatorCredentials.tryDelete(): Boolean {
        val cachedOperator = this
        return try {
            operatorCredentialsRepository.delete(cachedOperator)
            true
        } catch (ex: Exception) {
            yield()
            ex.rethrowIfFatal()
            logError("error when trying to delete $cachedOperator", ex)
            false
        }
    }

}