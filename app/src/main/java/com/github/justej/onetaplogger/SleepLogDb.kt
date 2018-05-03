package com.github.justej.onetaplogger

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.*
import android.arch.persistence.room.OnConflictStrategy.REPLACE
import android.arch.persistence.room.migration.Migration
import android.content.Context
import android.support.annotation.NonNull

@Entity(tableName = "sleepLog")
data class SleepLogData(@PrimaryKey(autoGenerate = true)
                        @ColumnInfo(name = "timestamp") var timestamp: Long,
                        @NonNull
                        @ColumnInfo(name = "label") var label: String,
                        @NonNull
                        @ColumnInfo(name = "comment") var comment: String = "") {
    constructor() : this(0, "", "")
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

    @Query("DELETE FROM sleepLog WHERE timestamp = :timestamp")
    fun delete(timestamp: Long)
}

@Database(entities = [(SleepLogData::class)], version = 2)
abstract class SleepLogDatabase : RoomDatabase() {
    abstract fun sleepLogDao(): SleepLogDao

    companion object {
        private var INSTANCE: SleepLogDatabase? = null

        fun getInstance(context: Context): SleepLogDatabase {
            if (INSTANCE == null) {
                synchronized(SleepLogDatabase::class) {
                    INSTANCE = Room.databaseBuilder(context, SleepLogDatabase::class.java, "SleepLog.db")
                            .addMigrations(Upgrade1to2())
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

    private class Upgrade1to2() : Migration(1, 2) {
        /**
         * Change DB schema:
         * - create a table with new schema
         * - migrate data to the new table
         * - drop the old table
         */
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE TEMPORARY TABLE sleepLog_backup(timestamp INTEGER PRIMARY KEY NOT NULL, label TEXT NOT NULL, comment TEXT NOT NULL)")
            database.execSQL("INSERT INTO sleepLog_backup SELECT timestamp, label, comment FROM sleepLog")
            database.execSQL("DROP TABLE sleepLog")
            database.execSQL("CREATE TABLE sleepLog(timestamp INTEGER PRIMARY KEY NOT NULL, label TEXT NOT NULL, comment TEXT NOT NULL)")
            database.execSQL("INSERT INTO sleepLog SELECT timestamp, label, comment FROM sleepLog_backup")
            database.execSQL("DROP TABLE sleepLog_backup")
        }
    }
}