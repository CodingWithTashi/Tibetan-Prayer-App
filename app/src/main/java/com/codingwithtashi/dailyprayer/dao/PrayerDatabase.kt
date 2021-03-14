package com.codingwithtashi.dailyprayer.dao

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.codingwithtashi.dailyprayer.di.ApplicationScope
import com.codingwithtashi.dailyprayer.model.Prayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

@Database(entities = [Prayer::class],version = 1)
abstract class PrayerDatabase : RoomDatabase() {
    abstract fun prayerDao(): PrayerDao;
    class Callback @Inject constructor (private val database:Provider<PrayerDatabase>,
    @ApplicationScope private val applicationScope:CoroutineScope):RoomDatabase.Callback(){
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            val dao =database.get().prayerDao()
            applicationScope.launch {
                dao.insert(Prayer(2,"Test1","Content","url",false))
                dao.insert(Prayer(5,"Test2","Content","url",false))
                dao.insert(Prayer(7,"Test3","asdsad","sada",false))
                dao.insert(Prayer(8,"Test4","Conastent","asdsa",false))
            }
        }
    }
}