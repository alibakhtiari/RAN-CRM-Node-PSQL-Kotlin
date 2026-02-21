package com.ran.crm.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ran.crm.data.local.dao.*
import com.ran.crm.data.local.entity.*

@Database(
        entities = [User::class, Contact::class, CallLog::class, SyncAudit::class],
        version = 6,
        exportSchema = true
)
abstract class CrmDatabase : RoomDatabase() {

    abstract fun contactDao(): ContactDao
    abstract fun callLogDao(): CallLogDao
    abstract fun syncAuditDao(): SyncAuditDao

    companion object {
        const val DATABASE_NAME = "crm_database"

        @Volatile private var INSTANCE: CrmDatabase? = null

        fun getDatabase(context: Context): CrmDatabase {
            return INSTANCE
                    ?: synchronized(this) {
                        val instance =
                                Room.databaseBuilder(
                                                context.applicationContext,
                                                CrmDatabase::class.java,
                                                DATABASE_NAME
                                        )
                                        // TODO: Add manual Migration objects when schema
                                        //  stabilises and user data must be preserved.
                                        .fallbackToDestructiveMigration(true)
                                        .build()
                        INSTANCE = instance
                        instance
                    }
        }
    }
}
