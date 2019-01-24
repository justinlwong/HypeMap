package justin.apackage.com.hypemap

import android.arch.lifecycle.MutableLiveData
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class InstagramRepository {
    private val gson: Gson by lazy { GsonBuilder().create()}
    private val instagramService : InstagramService by lazy {setupRetrofit()}
    private var instaData = InstagramData(mutableMapOf(), mutableMapOf())
    private var userMarkersMap: MutableMap<String, MutableList<Marker>> = mutableMapOf()
    private val newPostLiveData = MutableLiveData<Pair<String, MarkerPostData>>()
    private val usersLiveData = MutableLiveData<List<String>>()

    companion object {
        private const val TAG = "InstagramRepository"
    }

     fun getLatestPost() : MutableLiveData<Pair<String, MarkerPostData>> {
         return newPostLiveData
    }

    fun updatePosts() {
        removeAllMarkers()
        val newInstaData = InstagramData(mutableMapOf(), instaData.locationMap)
        if (instaData.usersPostData != null) {
            Observable.fromIterable(instaData?.usersPostData.asIterable())
                .flatMap {
                    instagramService.getUserPage(it.key)
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ response -> processUserProfileResponse(response, newInstaData) },
                    { error -> Log.d(TAG, "getPosts error: $error") },
                    { instaData = newInstaData }
                )
        }
    }

    private fun processUserProfileResponse(response: ResponseBody, iData: InstagramData) {
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

            val posts: JSONArray = user.getJSONObject("edge_owner_to_timeline_media")
                .getJSONArray("edges")

            val markerPosts: MutableMap<String, MarkerPostData> = mutableMapOf()
            for (i in 0..(posts.length() - 1)) {
                val node = posts.getJSONObject(i)
                    .getJSONObject("node")

                if (!node.isNull("location")) {
                    val location = node.getJSONObject("location")

                    Log.d(TAG,
                        "location: ${location.getString("name")} id: ${location.getString("id")}")

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

                    val newMarker = MarkerPostData(
                        user = userName,
                        name = location.getString("name"),
                        postUrl = posts.getJSONObject(i)//"https//www.instagram.com/p/${posts.getJSONObject(i)
                            .getJSONObject("node")
                            .getString("thumbnail_src"),
                        linkUrl = "https://www.instagram.com/p/${posts.getJSONObject(i)
                            .getJSONObject("node")
                            .getString("shortcode")}",
                        caption = captionText)

                    markerPosts[location.getString("id")] = newMarker
                }
            }

            val userPosts: Map<String, MarkerPostData> = markerPosts

            val newUserMarker = UserPosts(
                user.getString("profile_pic_url"),
                userPosts
            )

            iData.usersPostData[userName] = newUserMarker

            Observable.fromIterable(userPosts.asIterable())
                .flatMap {
                    instagramService.getCoordinates(it.key)}
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe ({response ->
                    processLocationResponse(response, iData, userName)},
                    { error -> Log.d(TAG, "location search error: $error")})

            Log.d(TAG, "Adding new user: $userName")
        } else {
            Log.e(TAG, "Request was bad")
        }
    }

    private fun processLocationResponse(response: ResponseBody, newInstaData: InstagramData, userName: String) {
        // parse json
        val json = response.string()
        if (json != null) {
            val jsonObj = JSONObject(json)

            val locationData = jsonObj.getJSONObject("graphql")
                .getJSONObject("location")

            val latLng = LatLng(locationData.getDouble("lat"), locationData.getDouble("lng"))
            val id: String = locationData.getString("id")
            Log.d(TAG, "location $id mapped to ${latLng.latitude}, ${latLng.longitude}")
            newInstaData.locationMap[id] = latLng
            newPostLiveData.postValue(Pair(id, newInstaData.usersPostData[userName]!!.posts[id]!!))
        } else {
            Log.d(TAG, "Bad location response")
        }
    }

    fun addUser(userName: String) {
        if (userName in instaData.usersPostData.keys) {
            instaData.usersPostData.remove(userName)
            removeUserMakers(userName)
        }
        instagramService.getUserPage(userName)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ response -> processUserProfileResponse(response, instaData) },
                { error -> Log.d(TAG, "getPosts error: $error") },
                { usersLiveData.postValue(instaData.usersPostData.keys.toList()) }
            )
    }

    fun getUsers() : MutableLiveData<List<String>> {
        return usersLiveData
    }

    fun removeAll() {
        instaData = InstagramData(mutableMapOf(), mutableMapOf())
        usersLiveData.postValue(instaData.usersPostData.keys.toList())
        removeAllMarkers()
    }

    fun removeAllMarkers() {
        for (userName in userMarkersMap.keys) {
            val userMarkers = userMarkersMap[userName]!!.toList()
            for (mkr in userMarkers) {
                mkr.remove()
            }
        }
    }

    fun hideUserMarkers(userName: String) {
        val userMarkers = userMarkersMap[userName]!!.toList()
        for (mkr in userMarkers) {
            mkr.isVisible = false
        }
    }

    fun showUserMarkers(userName: String) {
        val userMarkers = userMarkersMap[userName]!!.toList()
        for (mkr in userMarkers) {
            mkr.isVisible = true
        }
    }

    fun removeUserMakers(userName: String) {
        val userMarkers = userMarkersMap[userName]!!.toList()
        for (mkr in userMarkers) {
            mkr.remove()
        }
    }

    fun addMarker(userName: String, mkr: Marker) {
        if (userMarkersMap[userName] == null) {
            userMarkersMap[userName] = mutableListOf()
        }
        userMarkersMap[userName]!!.add(mkr)
    }

    fun getLocationMap() : Map<String, LatLng> {
        return instaData.locationMap
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