package com.el.konnekt.ui.activities

import android.app.Application
import androidx.room.Room
import com.el.konnekt.data.local.AppDatabase


class KonnektApp : Application() {
    companion object {
        lateinit var database: AppDatabase
            private set
    }

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "konnekt_db"
        ).fallbackToDestructiveMigration().build()
    }
}
