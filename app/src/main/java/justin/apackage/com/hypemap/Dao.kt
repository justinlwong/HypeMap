package justin.apackage.com.hypemap

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy.REPLACE
import android.arch.persistence.room.Query

@Dao
interface PostDao {
    @Query("SELECT * from posts")
    fun getPosts(): LiveData<List<Post>>

    @Query("SELECT * from posts WHERE userName = :userName")
    fun getUserPosts(userName: String): LiveData<List<Post>>

    @Insert(onConflict = REPLACE)
    fun insert(postData: Post)

    @Query("DELETE from posts")
    fun deleteAll()
}

@Dao
interface UserDao {
    @Query("SELECT * from users")
    fun getUsers(): LiveData<List<User>>

    @Insert(onConflict = REPLACE)
    fun insert(user: User)

    @Query("DELETE from users")
    fun deleteAll()
}

//@Dao
//interface LocationDao {
//    @Query("SELECT * from locations WHERE locationId = :locationId")
//    fun getCoordinates(locationId: String): Location
//
//    @Insert(onConflict = REPLACE)
//    fun insert(location: Location)
//
//    @Query("DELETE from locations")
//    fun deleteAll()
//}