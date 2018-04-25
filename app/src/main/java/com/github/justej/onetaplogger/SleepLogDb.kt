package com.github.justej.onetaplogger

import android.arch.persistence.room.*
import android.arch.persistence.room.OnConflictStrategy.REPLACE
import android.content.Context

@Entity(tableName = "sleepLog")
data class SleepLogData(@PrimaryKey(autoGenerate = true) var id: Long?,
                        @ColumnInfo(name = "timestamp") var timestamp: Long,
                        @ColumnInfo(name = "label") var label: String,
                        @ColumnInfo(name = "comment") var comment: String = "") {
    constructor() : this(null, 0, "", "")
}

@Dao
interface SleepLogDao {
    @Query("SELECT * from sleepLog ORDER BY timestamp DESC")
    fun get(): List<SleepLogData>

    @Query("SELECT * from sleepLog ORDER BY timestamp DESC LIMIT :limit")
    fun get(limit: Int): List<SleepLogData>

    @Query("SELECT * from sleepLog ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    fun get(limit: Int, offset: Int): List<SleepLogData>

    @Query("SELECT COUNT (*) from sleepLog")
    fun count(): Int

    @Insert
    fun insert(sleepLogData: SleepLogData)

    @Insert
    fun insert(sleepLogData: List<SleepLogData>)

    @Update(onConflict = REPLACE)
    fun update(sleepLogData: SleepLogData)

    @Query("DELETE FROM sleepLog")
    fun delete()
}

@Database(entities = [(SleepLogData::class)], version = 1)
abstract class SleepLogDatabase : RoomDatabase() {
    abstract fun sleepLogDao(): SleepLogDao

    companion object {
        private var INSTANCE: SleepLogDatabase? = null

        fun getInstance(context: Context): SleepLogDatabase {
            if (INSTANCE == null) {
                synchronized(SleepLogDatabase::class) {
                    INSTANCE = Room.databaseBuilder(context, SleepLogDatabase::class.java, "SleepLog.db")
                            .allowMainThreadQueries()
                            .build()
                }
            }
            return INSTANCE!!
        }

        fun destroyInstance() {
            INSTANCE = null
        }
    }
}