package justin.apackage.com.hypemap

import android.app.AlertDialog
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.FragmentTransaction
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class MapsActivity :
        AppCompatActivity(),
        OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener {
    
    private lateinit var mOverlayFragment: OverlayFragment
    private lateinit var mCurLocation : Location
    private lateinit var mLocationClient : FusedLocationProviderClient
    private lateinit var postPopupBuilder : AlertDialog.Builder
    private lateinit var postPopup : AlertDialog
    private lateinit var wv : WebView
    private val mModel by lazy {ViewModelProviders.of(this).get(HypeMapViewModel::class.java)}

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

        mOverlayFragment = OverlayFragment()
        val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.overlay_ui, mOverlayFragment)
        transaction.addToBackStack(null)
        transaction.commit()

        mLocationClient = LocationServices.getFusedLocationProviderClient(this)
        postPopupBuilder = AlertDialog.Builder(this)
        wv = WebView(this)
        wv.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false
            }
        }
        wv.setInitialScale(1)
        wv.settings.useWideViewPort = true
        wv.settings.loadWithOverviewMode = true
        postPopupBuilder.setView(wv)
        postPopup = postPopupBuilder.create()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mModel.mMap = googleMap
        mModel.mMap.uiSettings.isZoomControlsEnabled = true
        mModel.mMap.setOnMarkerClickListener(this)

        // Set to Toronto
        mModel.mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(43.6712698,-79.3819235), 1f))
        moveToCurrentLocation()

        mModel.getPosts().observe(this, Observer<List<Post>> { posts ->
            if (posts != null) {
                mModel.mMap.clear()
                for (post in posts) {
                    val id = post.locationId
                    val name = post.locationName
                    Log.d(TAG, "observing location id: $id and name: $name with coords: ${post.latitude}, ${post.longitude}")
                    addMarkerAtLocation(
                        LatLng(post.latitude, post.longitude),
                        name,
                        post)

                }
            }
        })
    }

    private fun addMarkerAtLocation(location: LatLng, locationName: String, postData: Post) {
        val markerOptions = MarkerOptions().position(location)
            .title(locationName)

        val mkr = mModel.mMap.addMarker(markerOptions)

        mkr.tag = postData
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
            mModel.mMap.isMyLocationEnabled = true

            mLocationClient.lastLocation.addOnSuccessListener(this) { location ->
                if (location != null) {
                    mCurLocation = location
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

    override fun onMarkerClick(p0: Marker?) : Boolean {
        if (p0 != null) {
            p0.showInfoWindow()
            mModel.mMap.setPadding(0, 0, 0, 1500)
            val zoom = mModel.mMap.cameraPosition.zoom
            var duration = 100f
            if (zoom != 0f) {
                duration = 200f * (12f / mModel.mMap.cameraPosition.zoom)
            }

            mModel.mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(p0.position, 12f), duration.toInt(), object : GoogleMap.CancelableCallback {
                override fun onCancel() {
                }

                override fun onFinish() {
                    if (p0.tag != null) {
                        val data = p0.tag as Post

                        wv.loadUrl(data.postUrl)
                        postPopup.setTitle(data.userName)
                        var caption = data.caption
                        if (caption.length > 100) {
                            caption = caption.substring(0, 100)
                        }
                        postPopup.setMessage(caption)
                        postPopup.setButton(
                            DialogInterface.BUTTON_NEUTRAL,
                            "View",
                            { _, _ ->
                                val url = data.linkUrl
                                val i = Intent(Intent.ACTION_VIEW)
                                i.data = Uri.parse(url)
                                startActivity(i)
                            }
                        )

                        postPopup.show()
                        val lp = WindowManager.LayoutParams()
                        lp.copyFrom(postPopup.window?.attributes)
                        lp.gravity = Gravity.BOTTOM
                        //lp.y = -1000
                        postPopup.window?.attributes = lp
                    }
                }
            })
            return true
        }
        return false
    }
}
