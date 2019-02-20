package justin.apackage.com.hypemap

import android.app.Application
import android.arch.lifecycle.LiveData
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class InstagramRepository(application: Application) {
    private val gson: Gson by lazy { GsonBuilder().create()}
    private val instagramService : InstagramService by lazy {setupRetrofit()}
    private val hypeMapDatabase = HypeMapDatabase.getInstance(application)
    private val postDao: PostDao = hypeMapDatabase?.postDao()!!
    private val userDao: UserDao = hypeMapDatabase?.userDao()!!
    private val mExecutor: Executor = Executors.newSingleThreadExecutor()

    companion object {
        private const val TAG = "InstagramRepository"
    }

    fun getPosts(): LiveData<List<Post>?> {
         return postDao.getPosts()
    }

    fun updatePostsPeriodically() {
        mExecutor.execute {
            val scheduler = Schedulers.from(Executors.newSingleThreadExecutor())
            val delay: Long = 0
            Observable.interval(delay, 2, TimeUnit.MINUTES, scheduler)
                .subscribe { n -> Log.d(TAG, "Updating periodically")
                    updatePosts()
                }
        }
    }

    private fun updatePosts() {
        mExecutor.execute{
            val usersList = userDao.getCurrentUsers()
            val posts: MutableList<Post> = mutableListOf()

            Observable.fromIterable(usersList.asIterable())
                        .flatMap { instagramService.getUserPage(it.userName)}
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.computation())
                        .subscribe({ response -> processUserProfileResponse(response, true, posts) },
                            { error -> Log.d(TAG, "getPosts error: $error") })
        }
    }

    private fun getCurrentUsersMap(): MutableMap<String, User> {
        val users = userDao.getCurrentUsers()
        val map: MutableMap<String, User> = mutableMapOf()
        for (user in users) {
            map[user.userName] = user
        }
        return map
    }

    private fun processUserProfileResponse(response: ResponseBody, isUpdate: Boolean, postsList: MutableList<Post>) {
        val html = response.string()
        if (html != null) {
            val document: Document = Jsoup.parse(html)
            val elements: Elements = document.getElementsByTag("script")
            lateinit var userData: String
            for (element in elements) {
                if (element.data().contains("window._sharedData")) {
                    Log.d(TAG, "Found string")
                    userData = element.data().substring(21)
                    break
                }
            }
            Log.d(TAG, userData.substring(0, 10))
            // parse json
            val profiles = JSONObject(userData)
                .getJSONObject("entry_data")
                .getJSONArray("ProfilePage")

            val user: JSONObject = profiles.getJSONObject(0)
                .getJSONObject("graphql")
                .getJSONObject("user")

            val userName = user.getString("username")

            if (!isUpdate) {
                // Update or add user
                val newUser = User(
                    userName = userName,
                    profilePicUrl = user.getString("profile_pic_url"),
                    visible = true,
                    colour = ((0..11).shuffled().first()*30).toFloat()
                )
                userDao.insert(newUser)
            }

            val posts: JSONArray = user.getJSONObject("edge_owner_to_timeline_media")
                .getJSONArray("edges")

            for (i in 0..(posts.length() - 1)) {
                val node = posts.getJSONObject(i)
                    .getJSONObject("node")

                if (!node.isNull("location")) {
                    val location = node.getJSONObject("location")

                    Log.d(
                        TAG,
                        "location: ${location.getString("name")} id: ${location.getString("id")}"
                    )

                    var captionText = ""
                    val captionEdge = posts.getJSONObject(i)
                        .getJSONObject("node")
                        .getJSONObject("edge_media_to_caption")

                    if (!captionEdge.isNull("edges")) {
                        captionText = captionEdge.getJSONArray("edges")
                            .getJSONObject(0)
                            .getJSONObject("node")
                            .getString("text")
                    }

                    val postId = node.getString("id")
                    if (postDao.getPost(postId).isEmpty()) {
                        Log.d(TAG, "Found new post")
                        val newPost = Post(
                            id = postId,
                            userName = userName,
                            locationName = location.getString("name"),
                            locationId = location.getString("id"),
                            longitude = 0.0,
                            latitude = 0.0,
                            postUrl = node
                                .getString("thumbnail_src"),
                            linkUrl = "https://www.instagram.com/p/${node
                                .getString("shortcode")}",
                            caption = captionText,
                            timestamp = node.getLong("taken_at_timestamp"),
                            visible = true,
                            colour = 0f
                        )
                        postsList.add(newPost)
                    }
                }
            }

            Observable.fromIterable(postsList.asIterable())
                .flatMap {
                    instagramService.getCoordinates(it.locationId)
                }
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .subscribe(
                    { response -> processLocationResponse(response, postsList)},
                    { error -> Log.d(TAG, "location search error: $error")})
        } else {
            Log.e(TAG, "Request was bad")
        }
    }

    @Synchronized fun insertPost(post: Post) {
        val userEntry = getCurrentUsersMap()[post.userName]
        if (userEntry != null) {
            post.colour = userEntry.colour
            postDao.insert(post)
        }
    }

    private fun processLocationResponse(response: ResponseBody, posts: MutableList<Post>) {
        // parse json
        val json = response.string()
        if (json != null) {
            val jsonObj = JSONObject(json)

            val locationData = jsonObj.getJSONObject("graphql")
                .getJSONObject("location")

            val latitude = locationData.getDouble("lat")
            val longitude = locationData.getDouble("lng")
            val id: String = locationData.getString("id")

            for (post in posts) {
                if (post.locationId == id) {
                    post.latitude = latitude
                    post.longitude = longitude
                    insertPost(post)
                }
            }

            Log.d(TAG, "location $id mapped to $latitude, $longitude")
        } else {
            Log.d(TAG, "Bad location response")
        }
    }

    fun addUser(userName: String) {
        mExecutor.execute{
            val posts: MutableList<Post> = mutableListOf()
            instagramService.getUserPage(userName)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .subscribe({ response -> processUserProfileResponse(response, false, posts) },
                    { error ->
                        Log.d(TAG, "addUser error: $error")
                    })
        }
    }

    fun removeUser(userName: String) {
        mExecutor.execute {
            userDao.delete(userName)
            postDao.delete(userName)
        }

    }

    fun getUsers() : LiveData<List<User>> {
        return userDao.getUsers()
    }

    fun removeAll() {
        mExecutor.execute{
            postDao.deleteAll()
            userDao.deleteAll()
        }
    }
    fun showUserMarkers(userName: String, visible: Boolean) {
        Log.d(TAG, "Setting $userName to visibility: $visible")
        mExecutor.execute{
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

    private fun setupRetrofit() : InstagramService {
        // Retrofit builder
        return Retrofit.Builder()
            .baseUrl(HypeMapConstants.IG_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()
            .create(InstagramService::class.java)
    }
}