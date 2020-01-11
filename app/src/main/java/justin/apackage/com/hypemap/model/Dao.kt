package justin.apackage.com.hypemap.model

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import androidx.room.Update

@Dao
interface PostDao {
    @Query("SELECT * from posts")
    fun getPosts(): LiveData<List<Post>>

    @Query("SELECT * from posts")
    fun getPostsBlocking(): List<Post>

    @Insert(onConflict = REPLACE)
    fun insert(postData: Post)

    @Update
    fun update(postData: Post)

    @Query("DELETE from posts")
    fun deleteAll()

    @Query("DELETE from posts WHERE userId = :userId")
    fun delete(userId: String)

    @Query("SELECT * from posts WHERE id = :postId")
    fun getPost(postId: String): List<Post>
}

@Dao
interface UserDao {
    @Query("SELECT * from users")
    fun getUsers(): LiveData<List<User>>

    @Query("SELECT * from users")
    fun getCurrentUsers(): List<User>

    @Query("SELECT * from users WHERE userId = :userId")
    fun getUser(userId: String): User

    @Insert(onConflict = REPLACE)
    fun insert(user: User)

    @Update
    fun update(user: User)

    @Query("DELETE from users WHERE userId = :userId")
    fun delete(userId: String)

    @Query("DELETE from users")
    fun deleteAll()
}

@Dao
interface LocationDao {
    @Query("SELECT * from locations WHERE locationId = :locationId")
    fun getLocation(locationId: String): Location?

    @Insert(onConflict = REPLACE)
    fun insert(location: Location)
}