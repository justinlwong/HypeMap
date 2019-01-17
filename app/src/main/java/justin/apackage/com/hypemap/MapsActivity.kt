package justin.apackage.com.hypemap

import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MapsActivity :
        AppCompatActivity(),
        OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener {
    override fun onMarkerClick(p0: Marker?) : Boolean {
        if (p0 != null) {
            p0.showInfoWindow()
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(p0.position, 12f))
            return true
        }
        return false
    }

    private lateinit var mMap: GoogleMap
    private lateinit var mCurLocation : Location
    private lateinit var mLocationClient : FusedLocationProviderClient
    private val gson: Gson by lazy {GsonBuilder().create()}
    private val instagramService: InstagramService by lazy {setupRetrofit()}
    private var mUserMarkers: MutableList<UserMarkers> = mutableListOf()
    private val mUserList : MutableList<String> = mutableListOf(
        "blogto",
        "mattshr",
        "jayscale",
        "visionelie",
        "tilore",
        "themelodyh"
        )

    companion object {
        private const val PERMISSION_LOCATION_REQUEST_CODE = 1
        private const val TAG = "MapsActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate called")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        mLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.setOnMarkerClickListener(this)

        moveToCurrentLocation()

        // TODO: In the future, input a list of user profiles here and only call this on refresh
        for (userName in mUserList) {
            getUserData(userName)
        }
    }

    private fun getUserData(userName: String) {
        val call = instagramService.getUserPage(userName)
        call.enqueue(object : Callback<ResponseBody> {
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.d(TAG, "onFailure getUserPage")
            }

            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.body() != null) {
                    val html = response.body()?.string()
                    if (html != null) {
                        //Log.d(TAG, "response: ${html.substring(0, 100)}")

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
                        Log.d(TAG, userData.substring(0,10))
                        // parse json
                        val profiles = JSONObject(userData)
                            .getJSONObject("entry_data")
                            .getJSONArray("ProfilePage")

                        val user : JSONObject = profiles.getJSONObject(0)
                            .getJSONObject("graphql")
                            .getJSONObject("user")

                        var posts : JSONArray = user.getJSONObject("edge_owner_to_timeline_media")
                            .getJSONArray("edges")

                        var markerPosts : MutableList<MarkerPostData> = mutableListOf()
                        for (i in 0..(posts.length() - 1)) {
                            val node = posts.getJSONObject(i)
                                .getJSONObject("node")

                            if (!node.isNull("location")) {
                                val location = node.getJSONObject("location")

                                Log.d(TAG, "location: ${location.getString("name")} id: ${location.getInt("id")}")
                                markerPosts.add(MarkerPostData(locationId = location.getInt("id"),
                                    name = location.getString("name"),
                                    latitude = null,
                                    longitude = null,
                                    postUrl = posts.getJSONObject(i)
                                        .getJSONObject("node")
                                        .getString("display_url")))
                            }
                        }

                        val newUserMarker = UserMarkers(
                            user.getString("full_name"),
                            user.getString("profile_pic_url"),
                            markerPosts)

                        mUserMarkers.add(newUserMarker)

                        Log.d(TAG, "Adding new user: ${newUserMarker.userName}")

                        getLocationCoordinates()

                    } else {
                        Log.e(TAG, "Request was bad")
                    }
                }
            }
        })
    }

    private fun getLocationCoordinates() {
        for (user in mUserMarkers) {
            for (location in user.posts) {
                Log.d(TAG, "Searching $location.name")
                val call = instagramService.getCoordinates(location.locationId.toString())
                Log.d(TAG, call.toString())
                call.enqueue(object : Callback<ResponseBody> {
                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        Log.d(TAG, "onFailure getCoordinates")
                    }

                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        if (response.body() != null) {
                            // parse json
                            val json = response.body()?.string()
                            if (json != null) {
                                val jsonObj = JSONObject(json)

                                val locationData = jsonObj.getJSONObject("graphql")
                                    .getJSONObject("location")
                                location.latitude = locationData.getDouble("lat")
                                location.longitude = locationData.getDouble("lng")
                                Log.d(TAG, "Updated $location.name with coords: " +
                                        "${location.latitude}, ${location.longitude})")

                                // Add Marker
                                val lat = location.latitude
                                val lng = location.longitude
                                if (lat != null && lng != null) {
                                    var loc = LatLng(lat, lng)
                                    addMarkerAtLocation(loc, location.name, location)
                                }

                            }
                        } else {
                            Log.e(TAG, "Request was bad")
                        }
                    }
                })
            }
        }
    }

    private fun setupRetrofit() : InstagramService {
        // Retrofit builder
        return Retrofit.Builder()
            .baseUrl(HypeMapConstants.IG_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(InstagramService::class.java)
    }

    private fun addMarkerAtLocation(location: LatLng, locationName: String, postData: MarkerPostData?) {
        val markerOptions = MarkerOptions().position(location)
            .title(locationName)

        val mkr = mMap.addMarker(markerOptions)

        if (postData != null) {
            mkr.setTag(postData)
        }
    }

    private fun moveToCurrentLocation() {
        Log.d(TAG, "Getting Permissions")
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "Getting permissions")
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSION_LOCATION_REQUEST_CODE)
            return
        } else {
            Log.d(TAG, "Set up current location on map")
            mMap.isMyLocationEnabled = true

            mLocationClient.lastLocation.addOnSuccessListener(this) { location ->
                if (location != null) {
                    mCurLocation = location
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    addMarkerAtLocation(currentLatLng, "You", null)
                    //mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 18f))
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_LOCATION_REQUEST_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    moveToCurrentLocation()
                }
                return
            }
        }
    }
}
