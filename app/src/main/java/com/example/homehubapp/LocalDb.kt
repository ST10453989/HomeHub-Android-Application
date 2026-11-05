package com.example.homehubapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.homehubapp.model.CreateServiceRequest
import com.example.homehubapp.model.ServiceDto

// A request that was created while offline and is waiting to be synced
data class PendingRequest(val localId: Int, val serviceName: String, val request: CreateServiceRequest)

// Local SQLite database: stores offline requests and a cached copy of the services list
class LocalDb private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "homehub_local.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE pending_requests (" +
                    "local_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "user_id INTEGER NOT NULL, " +
                    "service_id INTEGER NOT NULL, " +
                    "service_name TEXT NOT NULL, " +
                    "description TEXT NOT NULL, " +
                    "service_address TEXT NOT NULL, " +
                    "preferred_date TEXT NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE cached_services (" +
                    "id INTEGER PRIMARY KEY, " +
                    "name TEXT NOT NULL, " +
                    "description TEXT NOT NULL)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS pending_requests")
        db.execSQL("DROP TABLE IF EXISTS cached_services")
        onCreate(db)
    }

    // Save a request locally because the phone has no connection
    fun addPending(request: CreateServiceRequest, serviceName: String): Long {
        val values = ContentValues().apply {
            put("user_id", request.userId)
            put("service_id", request.serviceId)
            put("service_name", serviceName)
            put("description", request.description)
            put("service_address", request.serviceAddress)
            put("preferred_date", request.preferredDate)
        }
        return writableDatabase.insert("pending_requests", null, values)
    }

    // Oldest first, so requests sync in the order the user made them
    fun getPending(userId: Int? = null): List<PendingRequest> {
        val list = mutableListOf<PendingRequest>()
        val sql = "SELECT * FROM pending_requests" +
                (if (userId != null) " WHERE user_id = $userId" else "") +
                " ORDER BY local_id ASC"
        readableDatabase.rawQuery(sql, null).use { c ->
            while (c.moveToNext()) {
                list.add(
                    PendingRequest(
                        localId = c.getInt(c.getColumnIndexOrThrow("local_id")),
                        serviceName = c.getString(c.getColumnIndexOrThrow("service_name")),
                        request = CreateServiceRequest(
                            userId = c.getInt(c.getColumnIndexOrThrow("user_id")),
                            serviceId = c.getInt(c.getColumnIndexOrThrow("service_id")),
                            description = c.getString(c.getColumnIndexOrThrow("description")),
                            serviceAddress = c.getString(c.getColumnIndexOrThrow("service_address")),
                            preferredDate = c.getString(c.getColumnIndexOrThrow("preferred_date"))
                        )
                    )
                )
            }
        }
        return list
    }

    fun deletePending(localId: Int) {
        writableDatabase.delete("pending_requests", "local_id = ?", arrayOf(localId.toString()))
    }

    // Keep a copy of the services so the booking screen still works offline
    fun saveServices(services: List<ServiceDto>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("cached_services", null, null)
            services.forEach {
                val values = ContentValues().apply {
                    put("id", it.id)
                    put("name", it.name)
                    put("description", it.description)
                }
                db.insert("cached_services", null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getServices(): List<ServiceDto> {
        val list = mutableListOf<ServiceDto>()
        readableDatabase.rawQuery("SELECT * FROM cached_services", null).use { c ->
            while (c.moveToNext()) {
                list.add(
                    ServiceDto(
                        c.getInt(c.getColumnIndexOrThrow("id")),
                        c.getString(c.getColumnIndexOrThrow("name")),
                        c.getString(c.getColumnIndexOrThrow("description"))
                    )
                )
            }
        }
        return list
    }

    companion object {
        @Volatile private var instance: LocalDb? = null

        fun get(context: Context): LocalDb =
            instance ?: synchronized(this) {
                instance ?: LocalDb(context).also { instance = it }
            }
    }
}