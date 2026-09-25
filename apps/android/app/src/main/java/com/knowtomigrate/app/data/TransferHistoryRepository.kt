package com.knowtomigrate.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TransferHistoryRepository private constructor(context: Context) {

    private val dbHelper = TransferDatabaseHelper(context.applicationContext)
    private val scope = CoroutineScope(Dispatchers.IO)
    private val mutex = Mutex()

    private val _transfers = MutableStateFlow<List<TransferRecord>>(emptyList())
    val transfers: StateFlow<List<TransferRecord>> = _transfers.asStateFlow()

    init {
        refreshTransfers()
    }

    private fun refreshTransfers() {
        scope.launch {
            mutex.withLock {
                _transfers.value = loadAllFromDb()
            }
        }
    }

    fun insert(record: TransferRecord) {
        scope.launch {
            mutex.withLock {
                val db = dbHelper.writableDatabase
                val values = ContentValues().apply {
                    put("id", record.id)
                    put("session_id", record.sessionId)
                    put("file_name", record.fileName)
                    put("file_count", record.fileCount)
                    put("total_bytes", record.totalBytes)
                    put("bytes_transferred", record.bytesTransferred)
                    put("peer_name", record.peerDeviceName)
                    put("peer_id", record.peerDeviceId)
                    put("direction", record.direction.name)
                    put("status", record.status.name)
                    put("transport", record.transport)
                    put("timestamp", record.timestamp)
                    put("error_msg", record.errorMessage)
                }
                db.insertWithOnConflict(
                    TransferDatabaseHelper.TABLE_TRANSFERS,
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_REPLACE
                )
                _transfers.value = loadAllFromDb()
            }
        }
    }

    fun updateProgress(
        sessionId: String,
        status: TransferRecordStatus,
        bytesTransferred: Long,
        totalBytes: Long? = null,
        error: String? = null
    ) {
        scope.launch {
            mutex.withLock {
                val db = dbHelper.writableDatabase
                val values = ContentValues().apply {
                    put("status", status.name)
                    put("bytes_transferred", bytesTransferred)
                    if (totalBytes != null && totalBytes > 0) {
                        put("total_bytes", totalBytes)
                    }
                    if (error != null) {
                        put("error_msg", error)
                    }
                }
                db.update(
                    TransferDatabaseHelper.TABLE_TRANSFERS,
                    values,
                    "session_id = ?",
                    arrayOf(sessionId)
                )
                _transfers.value = loadAllFromDb()
            }
        }
    }

    fun delete(id: String) {
        scope.launch {
            mutex.withLock {
                val db = dbHelper.writableDatabase
                db.delete(TransferDatabaseHelper.TABLE_TRANSFERS, "id = ?", arrayOf(id))
                _transfers.value = loadAllFromDb()
            }
        }
    }

    fun clearAll() {
        scope.launch {
            mutex.withLock {
                val db = dbHelper.writableDatabase
                db.delete(TransferDatabaseHelper.TABLE_TRANSFERS, null, null)
                _transfers.value = emptyList()
            }
        }
    }

    private fun loadAllFromDb(): List<TransferRecord> {
        val list = mutableListOf<TransferRecord>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            TransferDatabaseHelper.TABLE_TRANSFERS,
            null,
            null,
            null,
            null,
            null,
            "timestamp DESC"
        )

        cursor.use { c ->
            val idIdx = c.getColumnIndex("id")
            val sessIdx = c.getColumnIndex("session_id")
            val nameIdx = c.getColumnIndex("file_name")
            val countIdx = c.getColumnIndex("file_count")
            val totalIdx = c.getColumnIndex("total_bytes")
            val transIdx = c.getColumnIndex("bytes_transferred")
            val peerNameIdx = c.getColumnIndex("peer_name")
            val peerIdIdx = c.getColumnIndex("peer_id")
            val dirIdx = c.getColumnIndex("direction")
            val statusIdx = c.getColumnIndex("status")
            val transTypeIdx = c.getColumnIndex("transport")
            val timeIdx = c.getColumnIndex("timestamp")
            val errIdx = c.getColumnIndex("error_msg")

            while (c.moveToNext()) {
                val dirStr = if (dirIdx != -1) c.getString(dirIdx) else "SENT"
                val statusStr = if (statusIdx != -1) c.getString(statusIdx) else "COMPLETED"

                val direction = try {
                    TransferDirection.valueOf(dirStr)
                } catch (_: Exception) {
                    TransferDirection.SENT
                }

                val status = try {
                    TransferRecordStatus.valueOf(statusStr)
                } catch (_: Exception) {
                    TransferRecordStatus.COMPLETED
                }

                list.add(
                    TransferRecord(
                        id = if (idIdx != -1) c.getString(idIdx) else "",
                        sessionId = if (sessIdx != -1) c.getString(sessIdx) else "",
                        fileName = if (nameIdx != -1) c.getString(nameIdx) else "File",
                        fileCount = if (countIdx != -1) c.getInt(countIdx) else 1,
                        totalBytes = if (totalIdx != -1) c.getLong(totalIdx) else 0L,
                        bytesTransferred = if (transIdx != -1) c.getLong(transIdx) else 0L,
                        peerDeviceName = if (peerNameIdx != -1) c.getString(peerNameIdx) else "Device",
                        peerDeviceId = if (peerIdIdx != -1) c.getString(peerIdIdx) else "",
                        direction = direction,
                        status = status,
                        transport = if (transTypeIdx != -1) c.getString(transTypeIdx) else "WIFI_LAN",
                        timestamp = if (timeIdx != -1) c.getLong(timeIdx) else System.currentTimeMillis(),
                        errorMessage = if (errIdx != -1) c.getString(errIdx) else null
                    )
                )
            }
        }
        return list
    }

    private class TransferDatabaseHelper(context: Context) :
        SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE $TABLE_TRANSFERS (
                    id TEXT PRIMARY KEY,
                    session_id TEXT,
                    file_name TEXT,
                    file_count INTEGER,
                    total_bytes INTEGER,
                    bytes_transferred INTEGER,
                    peer_name TEXT,
                    peer_id TEXT,
                    direction TEXT,
                    status TEXT,
                    transport TEXT,
                    timestamp INTEGER,
                    error_msg TEXT
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX idx_transfers_timestamp ON $TABLE_TRANSFERS (timestamp DESC)")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS $TABLE_TRANSFERS")
            onCreate(db)
        }

        companion object {
            const val DATABASE_NAME = "ktm_history.db"
            const val DATABASE_VERSION = 1
            const val TABLE_TRANSFERS = "transfers"
        }
    }

    companion object {
        @Volatile
        private var instance: TransferHistoryRepository? = null

        fun getInstance(context: Context): TransferHistoryRepository {
            return instance ?: synchronized(this) {
                instance ?: TransferHistoryRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
