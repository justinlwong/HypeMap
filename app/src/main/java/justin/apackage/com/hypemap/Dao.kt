package justin.apackage.com.hypemap

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy.REPLACE
import android.arch.persistence.room.Query
import android.arch.persistence.room.Update

@Dao
interface PostDao {
    @Query("SELECT * from posts")
    fun getPosts(): LiveData<List<Post>>

    @Query("SELECT * from posts WHERE userName = :userName")
    fun getUserPosts(userName: String): List<Post>

    @Insert(onConflict = REPLACE)
    fun insert(postData: Post)

    @Update
    fun update(postData: Post)

    @Query("DELETE from posts")
    fun deleteAll()
}

@Dao
interface UserDao {
    @Query("SELECT * from users")
    fun getUsers(): LiveData<List<User>>

    @Query("SELECT * from users WHERE userName = :userName")
    fun getUser(userName: String): User

    @Insert(onConflict = REPLACE)
    fun insert(user: User)

    @Update
    fun update(user: User)

    @Query("DELETE from users")
    fun deleteAll()
}