package com.example.guardiantrackapp.data.repository

import com.example.guardiantrackapp.data.local.db.dao.EmergencyContactDao
import com.example.guardiantrackapp.data.local.db.entity.EmergencyContactEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactRepository @Inject constructor(
    private val contactDao: EmergencyContactDao
) {
    fun getAllContacts(): Flow<List<EmergencyContactEntity>> {
        return contactDao.getAllContacts()
    }

    suspend fun insertContact(contact: EmergencyContactEntity): Long {
        return contactDao.insertContact(contact)
    }

    suspend fun deleteContact(contact: EmergencyContactEntity) {
        contactDao.deleteContact(contact)
    }

    suspend fun deleteContactById(id: Long) {
        contactDao.deleteContactById(id)
    }

    suspend fun getAllContactsList(): List<EmergencyContactEntity> {
        return contactDao.getAllContactsList()
    }
}
