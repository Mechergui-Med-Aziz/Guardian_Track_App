package com.example.guardiantrackapp.provider

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import com.example.guardiantrackapp.data.local.db.GuardianDatabase
import com.example.guardiantrackapp.data.local.db.dao.EmergencyContactDao
import com.example.guardiantrackapp.data.local.db.entity.EmergencyContactEntity
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent


class EmergencyContactsProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = "com.guardian.track.provider"
        const val PATH_CONTACTS = "emergency_contacts"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/$PATH_CONTACTS")

        private const val CONTACTS = 1
        private const val CONTACT_ID = 2

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, PATH_CONTACTS, CONTACTS)
            addURI(AUTHORITY, "$PATH_CONTACTS/#", CONTACT_ID)
        }
    }

    // Hilt EntryPoint for getting DAO in ContentProvider (ContentProviders cannot use @AndroidEntryPoint)
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ProviderEntryPoint {
        fun emergencyContactDao(): EmergencyContactDao
    }

    private lateinit var contactDao: EmergencyContactDao

    override fun onCreate(): Boolean {
        return true
    }

    private fun getDao(): EmergencyContactDao {
        if (!::contactDao.isInitialized) {
            val appContext = context?.applicationContext ?: throw IllegalStateException("Context is null")
            val entryPoint = EntryPointAccessors.fromApplication(
                appContext,
                ProviderEntryPoint::class.java
            )
            contactDao = entryPoint.emergencyContactDao()
        }
        return contactDao
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        val dao = getDao()
        return when (uriMatcher.match(uri)) {
            CONTACTS -> dao.getAllContactsCursor()
            CONTACT_ID -> {
                val id = ContentUris.parseId(uri)
                dao.getContactCursorById(id)
            }
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    override fun getType(uri: Uri): String {
        return when (uriMatcher.match(uri)) {
            CONTACTS -> "vnd.android.cursor.dir/vnd.$AUTHORITY.$PATH_CONTACTS"
            CONTACT_ID -> "vnd.android.cursor.item/vnd.$AUTHORITY.$PATH_CONTACTS"
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        if (uriMatcher.match(uri) != CONTACTS) {
            throw IllegalArgumentException("Invalid URI for insert: $uri")
        }

        val name = values?.getAsString("name") ?: return null
        val phoneNumber = values.getAsString("phone_number") ?: return null

        val entity = EmergencyContactEntity(
            name = name,
            phoneNumber = phoneNumber
        )

        val dao = getDao()
        val id = dao.insertContactSync(entity)
        context?.contentResolver?.notifyChange(uri, null)
        return ContentUris.withAppendedId(CONTENT_URI, id)
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        val dao = getDao()
        return when (uriMatcher.match(uri)) {
            CONTACT_ID -> {
                val id = ContentUris.parseId(uri)
                val count = dao.deleteContactByIdSync(id)
                if (count > 0) {
                    context?.contentResolver?.notifyChange(uri, null)
                }
                count
            }
            else -> throw IllegalArgumentException("Invalid URI for delete: $uri")
        }
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        // Update not required by spec, but implementing minimal support
        return 0
    }
}
