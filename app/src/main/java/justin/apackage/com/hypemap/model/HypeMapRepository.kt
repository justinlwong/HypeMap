package justin.apackage.com.hypemap.model

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
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

/**
 * A repository class for managing local data and network requests
 *
 * @author Justin Wong
 */
@SuppressLint("CheckResult")
class HypeMapRepository(private val application: Application) {
    private val instagramService : InstagramService by lazy {setupRetrofit()}
    private val hypeMapDatabase = HypeMapDatabase.getInstance(application)
    private val postDao: PostDao = hypeMapDatabase!!.postDao()
    private val userDao: UserDao = hypeMapDatabase!!.userDao()
    private val locationDao: LocationDao = hypeMapDatabase!!.locationDao()
    private val ioScheduler: Scheduler = Schedulers.io()

    companion object {
        private const val TAG = "HypeMapRepository"
    }

    fun getPosts(): LiveData<List<PostLocation>> {
         return postDao.getPosts()
    }

    fun getPostsBlocking(): List<PostLocation> {
        return postDao.getPostsBlocking()
    }

    fun getUserPosts(userName: String): List<PostLocation> {
        return postDao.getUserPosts(userName)
    }

    fun updatePosts() {
        ioScheduler.scheduleDirect {
            fetchPosts(userDao.getCurrentUsers())
                .observeOn(Schedulers.computation())
                .subscribe()
        }
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

    private fun fetchPosts(users: List<User>): Observable<Pair<RawPost, Location>> {
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
                    val location = locationDao.getLocation(post.locationId)
                    if (location != null) {
                        Single.just(location).toObservable()
                    } else {
                        instagramService
                            .getCoordinates(getCookie(INSTAGRAM_COOKIE_KEY), post.locationId)
                            .flatMap { response ->
                                val bodyString = response.body()?.string()
                                Single.just(Parser.getLocation(bodyString))
                            }
                            .toObservable()
                    }
                },
                { post: RawPost, location: Location ->
                    locationDao.insert(location)
                    Pair(post, location)})
            .onErrorResumeNext(Observable.empty())
            .doOnNext {
                insertPost(it.first, it.second)
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
    private fun insertPost(post: RawPost, location: Location) {
        val postLocation = PostLocation(
            post.id,
            post.userName,
            post.locationName,
            post.locationId,
            location.latitude,
            location.longitude,
            post.postUrl,
            post.linkUrl,
            post.caption,
            post.timestamp)
        postDao.insert(postLocation)
    }

    fun getUsers() : LiveData<List<User>> {
        return userDao.getUsers()
    }

    private fun getCookie(key: String) : String {
        val preferences = application.getSharedPreferences(HYPEMAP_SHARED_PREF, Context.MODE_PRIVATE)
        return preferences.getString(key, "") ?: ""
    }

    private fun setupRetrofit() : InstagramService {
        val gson = GsonBuilder().setLenient().create()
        // Retrofit builder
        return Retrofit.Builder()
            .baseUrl(HypeMapConstants.IG_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()
            .create(InstagramService::class.java)
    }
}