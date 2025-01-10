package com.idi.vaccinetracker.common.data.database.repositories

import com.idi.vaccinetracker.common.data.database.daos.OperatorCredentialsDao
import com.idi.vaccinetracker.common.data.database.entities.toDomain
import com.idi.vaccinetracker.common.data.database.entities.toPersistence
import com.idi.vaccinetracker.common.data.database.repositories.base.RepositoryBase
import com.idi.vaccinetracker.common.domain.entities.OperatorCredentials
import com.idi.vaccinetracker.common.exceptions.InsertEntityException
import com.idi.vaccinetracker.common.helpers.logInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class OperatorCredentialsRepository @Inject constructor(
    private val operatorCredentialsDao: OperatorCredentialsDao,
) : RepositoryBase<OperatorCredentials> {

    fun observeUsername(username: String): Flow<OperatorCredentials?> {
        return operatorCredentialsDao.observeChanges()
            .map { operatorCredentialsDao.findByUsername(username) }
            .map { it?.toDomain() }
    }

    suspend fun findByUsername(username: String): OperatorCredentials? {
        return operatorCredentialsDao.findByUsername(username)?.toDomain()
    }

    suspend fun findAll(): List<OperatorCredentials> {
        return operatorCredentialsDao.findAll().map { it.toDomain() }
    }

    suspend fun delete(operatorCredentials: OperatorCredentials) {
        val entity = operatorCredentials.toPersistence()
        val success = operatorCredentialsDao.delete(entity) > 0
        logInfo("delete ${operatorCredentials.username} $success")
    }

    override suspend fun insert(model: OperatorCredentials, orReplace: Boolean) {
        val entity = model.toPersistence()
        val success = operatorCredentialsDao.run {
            if (orReplace) insertOrReplace(entity) else insert(entity)
        } > 0L
        if (!success) {
            throw InsertEntityException("could not insert operator credentials", orReplace = orReplace)
        }
    }

    override fun observeChanges(): Flow<Long> {
        return operatorCredentialsDao.observeChanges()
    }
}