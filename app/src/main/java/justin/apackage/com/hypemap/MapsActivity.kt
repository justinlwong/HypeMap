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
    override fun onMarkerClick(p0: Marker?) = false

    private lateinit var mMap: GoogleMap
    private lateinit var mCurLocation : Location
    private lateinit var mLocationClient : FusedLocationProviderClient
    private val gson: Gson by lazy {GsonBuilder().create()}
    private val instagramService: InstagramService by lazy {setupRetrofit()}

    companion object {
        private const val PERMISSION_LOCATION_REQUEST_CODE = 1
        private const val PLACE_PICKER_REQUEST = 2
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

        val call = instagramService.getUserPage("blogto")
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

                        val posts : JSONArray = profiles.getJSONObject(0)
                            .getJSONObject("graphql")
                            .getJSONObject("user")
                            .getJSONObject("edge_owner_to_timeline_media")
                            .getJSONArray("edges")

                        for (i in 0..(posts.length() - 1)) {
                            val locationName = posts.getJSONObject(i)
                                .getJSONObject("node")
                                .getJSONObject("location")
                                .getString("name")
                            Log.d(TAG, "location: $locationName")
                        }
                    } else {
                        Log.e(TAG, "Request was bad")
                    }
                }
            }
        })
    }

    private fun setupRetrofit() : InstagramService {
        // Retrofit builder
        return Retrofit.Builder()
            .baseUrl(HypeMapConstants.IG_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(InstagramService::class.java)
    }

    private fun addMarkerAtLocation(location: LatLng) {
        val markerOptions = MarkerOptions().position(location)
        mMap.addMarker(markerOptions)
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
                    addMarkerAtLocation(currentLatLng)
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 18f))
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
