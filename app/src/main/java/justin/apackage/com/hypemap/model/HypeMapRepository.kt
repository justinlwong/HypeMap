package justin.apackage.com.hypemap.model

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import justin.apackage.com.hypemap.HypeMapConstants
import justin.apackage.com.hypemap.HypeMapConstants.Companion.HYPEMAP_SHARED_PREF
import justin.apackage.com.hypemap.HypeMapConstants.Companion.INSTAGRAM_COOKIE_KEY
import justin.apackage.com.hypemap.network.InstagramService
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * A repository class for managing local data and network requests
 *
 * @author Justin Wong
 */
@SuppressLint("CheckResult")
class HypeMapRepository(private val application: Application) {
    private val gson: Gson by lazy { GsonBuilder().create()}
    private val instagramService : InstagramService by lazy {setupRetrofit()}
    private val hypeMapDatabase = HypeMapDatabase.getInstance(application)
    private val postDao: PostDao = hypeMapDatabase?.postDao()!!
    private val userDao: UserDao = hypeMapDatabase?.userDao()!!
    private val ioScheduler: Scheduler = Schedulers.io()

    companion object {
        private const val TAG = "HypeMapRepository"
    }

    fun getPosts(): LiveData<List<PostLocation>?> {
         return postDao.getPosts()
    }

    fun updatePostsPeriodically() {
        val scheduler = Schedulers.from(Executors.newSingleThreadExecutor())
        val delay: Long = 0
        Observable.interval(delay, 2, TimeUnit.MINUTES, scheduler)
            .subscribe { n -> Log.d(TAG, "Updating periodically")
                updatePosts()
            }
    }

    fun updatePosts() {
        ioScheduler.scheduleDirect {
            fetchPosts(userDao.getCurrentUsers())
                .observeOn(Schedulers.computation())
                .subscribe()
        }
    }

    fun updatePost(post: PostLocation) {
        postDao.update(post)
    }

    fun addUser(userName: String) {
        ioScheduler.scheduleDirect {
            fetchUser(userName)
            .flatMapObservable { response ->
                fetchPosts(listOf(userDao.getUser(userName)))
            }
            .subscribeOn(ioScheduler)
            .observeOn(Schedulers.computation())
            .subscribe()
        }
    }

    private fun fetchPosts(users: List<User>): Observable<Pair<RawPost, Response<ResponseBody>>> {
        return Observable.defer{ Observable.just(users)}
            .flatMapIterable { usersList -> usersList }
            .flatMapSingle { instagramService.getUserPage(getCookie(INSTAGRAM_COOKIE_KEY), it.userName)}
            .flatMapMaybe { response ->
                val userWrapper: UserWrapper? = getUserWrapper(response.body()?.string())
                userWrapper?.let {
                    Maybe.just(it.posts)
                }
            }
            .flatMapIterable { posts -> posts }
            .flatMap(
                { post ->
                    instagramService.getCoordinates(getCookie(INSTAGRAM_COOKIE_KEY), post.locationId).toObservable()},
                { post: RawPost, response: Response<ResponseBody> -> Pair(post, response)})
            .onErrorResumeNext(Observable.empty())
            .doOnNext { result ->
                insertPost(result.first, result.second.body()?.string())
            }
            .subscribeOn(ioScheduler)
    }

    private fun fetchUser(userName: String): Single<Response<ResponseBody>> {
        return instagramService.getUserPage(getCookie(INSTAGRAM_COOKIE_KEY), userName)
            .doOnSuccess { response ->
                val wrapper = getUserWrapper(response.body()?.string())
                wrapper?.let {
                    userDao.insert(it.user)
                }
            }
            .doOnError { error ->
                Log.d(TAG, "addUser error: $error") }
            .subscribeOn(ioScheduler)
    }

    private fun getUserWrapper(response: String?): UserWrapper? {
        response?.let {
            return Parser.getUserWrapper(response)
        } ?: Log.e(TAG, "User profile request was bad")

        return null
    }

    @Synchronized
    private fun insertPost(post: RawPost, response: String?) {
        Parser.getLocation(post, response)?.let { postLocation ->
            postLocation.visible = userDao.getUser(postLocation.userName).visible
            postDao.insert(postLocation)
        }
    }

    fun removeUser(userName: String) {
        ioScheduler.scheduleDirect {
            userDao.delete(userName)
            postDao.delete(userName)
        }

    }

    fun getUsers() : LiveData<List<User>> {
        return userDao.getUsers()
    }

    fun getUser(userName: String) : User {
        return userDao.getUser(userName)
    }

    fun updateUser(user: User) {
        userDao.update(user)
    }

    fun removeAll() {
        ioScheduler.scheduleDirect {
            postDao.deleteAll()
            userDao.deleteAll()
        }
    }

    fun showUserMarkers(userName: String, visible: Boolean) {
        Log.d(TAG, "Setting $userName to visibility: $visible")
        ioScheduler.scheduleDirect {
            val user = userDao.getUser(userName)
            user.visible = visible
            val posts = postDao.getUserPosts(userName)
            for (post in posts) {
                Log.d(TAG, "Overwriting ${post.locationName}")
                post.visible = visible
                postDao.update(post)
            }
            userDao.update(user)
        }
    }

    fun filterMarkersByTime(timeThreshold: Long) {
        ioScheduler.scheduleDirect {
            val users = userDao.getCurrentUsers()
            for (user in users) {
                val posts = postDao.getUserPosts(user.userName)
                for (post in posts) {
                    Log.d(TAG, "Overwriting ${post.locationName}")
                    val postTime = post.timestamp
                    if (postTime < timeThreshold) {
                        post.visible = false
                    } else {
                        post.visible = user.visible
                    }
                    postDao.update(post)
                }
                userDao.update(user)
            }
        }
    }

    private fun setupRetrofit() : InstagramService {
        // Retrofit builder
        return Retrofit.Builder()
            .baseUrl(HypeMapConstants.IG_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()
            .create(InstagramService::class.java)
    }

    private fun getCookie(key: String) : String {
        val preferences = application.getSharedPreferences(HYPEMAP_SHARED_PREF, Context.MODE_PRIVATE)
        return preferences.getString(key, "") ?: ""
    }
}