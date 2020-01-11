package justin.apackage.com.hypemap.model

import android.app.Application
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Post::class, User::class, Location::class], version = 1)
abstract class HypeMapDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
    abstract fun userDao(): UserDao
    abstract fun locationDao(): LocationDao

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