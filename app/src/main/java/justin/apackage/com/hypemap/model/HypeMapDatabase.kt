package justin.apackage.com.hypemap.model

import android.app.Application
import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase

@Database(entities = [PostLocation::class, User::class], version = 1)
abstract class HypeMapDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
    abstract fun userDao(): UserDao

    companion object {
        private var INSTANCE: HypeMapDatabase? = null

        fun getInstance(application: Application): HypeMapDatabase? {
            if (INSTANCE == null) {
                synchronized(HypeMapDatabase::class) {
                    INSTANCE = Room.databaseBuilder(application.applicationContext,
                        HypeMapDatabase::class.java, "hypemap.db")
                        .build()
                }
            }
            return INSTANCE
        }

        fun destroyInstance() {
            INSTANCE = null
        }
    }

}