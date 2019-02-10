package justin.apackage.com.hypemap

import android.app.AlertDialog
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.FragmentTransaction
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import java.util.*
import kotlin.concurrent.schedule

class MapsActivity :
        AppCompatActivity(),
        OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener {
    
    private lateinit var mOverlayFragment: OverlayFragment
    private lateinit var postPopupBuilder : AlertDialog.Builder
    private lateinit var postPopup : AlertDialog
    private lateinit var wv : WebView
    private val mModel by lazy {ViewModelProviders.of(this).get(HypeMapViewModel::class.java)}

    companion object {
        private const val TAG = "MapsActivity"
    }


    private fun callUpdateData() {
        Timer("updateInstaData", false).schedule(delay = 0, period = 1000*60*30) {
            mModel.updateInstaData()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate called")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        mOverlayFragment = OverlayFragment.newInstance()
        val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.overlay_ui, mOverlayFragment)
        transaction.commit()

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
        callUpdateData()

        mModel.getPosts().observe(this, Observer { posts ->
            posts?.let{
                mModel.mMap.clear()
                for (post in posts) {
                    val id = post.locationId
                    val name = post.locationName
                    Log.d(
                        TAG,
                        "observing location id: $id and name: $name with coords: ${post.latitude}, ${post.longitude}")
                    if (post.visible) {
                        addMarkerAtLocation(
                            LatLng(post.latitude, post.longitude),
                            name,
                            post
                        )
                    }
                }
            }
        })
    }

    private fun addMarkerAtLocation(location: LatLng, locationName: String, postData: Post): Marker {
        val markerOptions = MarkerOptions().position(location)
            .title(locationName)
            .icon(BitmapDescriptorFactory.defaultMarker(postData.colour))

        val mkr = mModel.mMap.addMarker(markerOptions)
        mkr.tag = postData
        return mkr
    }

    private fun loadPopup(postUrl: String, linkUrl: String, userName: String, caption: String) {
        wv.loadUrl(postUrl)
        postPopup.hide()
        postPopup.setTitle(userName)
        var captionStr: String = caption
        if (caption.length > 100) {
            captionStr = caption.substring(0, 100)
        }
        postPopup.setMessage(captionStr)
        postPopup.setButton(
            DialogInterface.BUTTON_NEUTRAL,
            "View")
        { _, _ ->
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(linkUrl)
            startActivity(i)
        }

        postPopup.setButton(
            DialogInterface.BUTTON_NEGATIVE,
            "Close")
        { _, _ ->
            postPopup.hide()
        }

        postPopup.setOnDismissListener{
            mModel.mMap.setPadding(0, 0, 0, 0)
        }

        postPopup.show()
        val lp = WindowManager.LayoutParams()
        lp.copyFrom(postPopup.window?.attributes)
        lp.gravity = Gravity.BOTTOM

        postPopup.window?.attributes = lp
    }

    override fun onMarkerClick(p0: Marker?) : Boolean {
        val post = p0?.tag as Post
        val postUrl = post.postUrl
        val linkUrl = post.linkUrl
        val userName = post.userName
        val caption = post.caption

        p0.showInfoWindow()
        mModel.mMap.setPadding(0, 0, 0, 1300)
        val zoom = mModel.mMap.cameraPosition.zoom
        var duration = 100f
        if (zoom != 0f) {
            duration = 200f * (12f / mModel.mMap.cameraPosition.zoom)
        }

        mModel.mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(p0.position, 16f), duration.toInt(), object : GoogleMap.CancelableCallback {
            override fun onCancel() {
                loadPopup(postUrl, linkUrl, userName, caption)
            }

            override fun onFinish() {
                loadPopup(postUrl, linkUrl, userName, caption)
            }
        })
        return true
    }
}
