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
import com.google.maps.android.ui.IconGenerator

class MapsActivity :
        AppCompatActivity(),
        OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnCameraMoveListener {

    private lateinit var mOverlayFragment: OverlayFragment
    private val mModel by lazy {ViewModelProviders.of(this).get(HypeMapViewModel::class.java)}
    private lateinit var webView: WebView
    private lateinit var popUp: AlertDialog
    private val iconFactory by lazy{IconGenerator(this)}
    private val markers: MutableList<Marker> = mutableListOf()
    private val infoMarkers: MutableList<Marker> = mutableListOf()

    companion object {
        private const val TAG = "MapsActivity"
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

        webView = createWebView()
        popUp = createPopUp()
        iconFactory.setTextAppearance(R.style.TextInfoWindow)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mModel.mMap = googleMap
        mModel.mMap.uiSettings.isZoomControlsEnabled = true
        mModel.mMap.setOnMarkerClickListener(this)
        mModel.mMap.setOnCameraMoveListener(this)

        // Set to Toronto
        mModel.mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(43.6712698,-79.3819235), 1f))

        mModel.updateInstaData()

        mModel.getPosts().observe(this, Observer { posts ->
            posts?.let{
                mModel.mMap.clear()
                markers.clear()
                infoMarkers.clear()
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

    private fun addMarkerAtLocation(location: LatLng, locationName: String, postData: Post) {
        val baseMarkerOptions = MarkerOptions().position(location)
        val pinMarkerOptions = baseMarkerOptions
            .icon(BitmapDescriptorFactory.defaultMarker(postData.colour))

        val mkr = mModel.mMap.addMarker(pinMarkerOptions)
        mkr.tag = postData
        markers.add(mkr)

        val infoMkr = mModel.mMap.addMarker(baseMarkerOptions.anchor(0.5f, 2.25f))
        infoMkr.setIcon(BitmapDescriptorFactory.fromBitmap(iconFactory.makeIcon(locationName)))
        infoMkr.tag = postData
        infoMkr.isVisible = false
        infoMarkers.add(infoMkr)
        updateInfoBasedOnZoom()
    }

    private fun getTrimmedCaption(caption: String): String {
        var captionStr: String = caption
        if (caption.length > 100) {
            captionStr = caption.substring(0, 100)
        }
        return captionStr
    }

    private fun createWebView(): WebView{
        val webView = WebView(this)
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false
            }
        }

        webView.setInitialScale(1)
        webView.settings.useWideViewPort = true
        webView.settings.loadWithOverviewMode = true

        return webView
    }

    private fun createPopUp(): AlertDialog {
        val postPopupBuilder = AlertDialog.Builder(this)

        postPopupBuilder.setView(webView)

        postPopupBuilder.setNegativeButton(
            "Close")
        { dialog, _ ->
            dialog.dismiss()
        }

        postPopupBuilder.setOnDismissListener {
            mModel.mMap.setPadding(0, 0, 0, 0)
        }

        return postPopupBuilder.create()
    }

    @Synchronized
    private fun showPopup(postUrl: String, linkUrl: String, userName: String, caption: String) {
        if (popUp.isShowing) {
            popUp.dismiss()
        }

        webView.loadUrl(postUrl)
        popUp.setTitle(userName)
        popUp.setMessage(getTrimmedCaption(caption))
        popUp.setButton(
            DialogInterface.BUTTON_NEUTRAL,
            "View")
        { _, _ ->
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(linkUrl)
            startActivity(i)
        }

        popUp.window?.run{
            val lp = WindowManager.LayoutParams()
            lp.copyFrom(attributes)
            lp.gravity = Gravity.BOTTOM
            //setDimAmount(0.0f)
            attributes = lp
        }

        popUp.show()
        Log.d(TAG, "Showing popup")
    }

    override fun onMarkerClick(p0: Marker?) : Boolean {
        p0?.let { marker ->
            mModel.mMap.setPadding(0, 0, 0, 1300)
            val zoom = mModel.mMap.cameraPosition.zoom
            var duration = 100f
            if (zoom != 0f) {
                duration = 300f * (12f / mModel.mMap.cameraPosition.zoom)
            }

            mModel.mMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(marker.position, 16f),
                duration.toInt(),
                object : GoogleMap.CancelableCallback {
                    override fun onCancel() {
                    }

                    override fun onFinish() {
                        marker.tag?.let { tag ->
                            val post: Post = tag as Post
                            post.run {
                                Log.d(TAG, "Finished animation, Post info: $caption, $userName")
                                showPopup(postUrl, linkUrl, userName, caption)
                            }
                        }

                    }
                })
        }
        return true
    }

    private fun showInfoMarkers(show: Boolean) {
        for (infoMarker in infoMarkers) {
            infoMarker.isVisible = show
        }
    }

    private fun updateInfoBasedOnZoom() {
        val zoom = mModel.mMap.cameraPosition.zoom
        when {
            zoom > 14f -> showInfoMarkers(true)
            else -> showInfoMarkers(false)
        }
    }

    override fun onCameraMove() {
        Log.d(TAG, "Camera moved")
        updateInfoBasedOnZoom()
    }
}
