package justin.apackage.com.hypemap.ui

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import justin.apackage.com.hypemap.R
import justin.apackage.com.hypemap.model.HypeMapViewModel
import justin.apackage.com.hypemap.model.PostLocation

/**
 * The core activity which shows the map and instagram post data as clickable markers
 *
 * @author Justin Wong
 */
class MapsActivity :
        AppCompatActivity(),
        OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnCameraMoveListener,
        GoogleMap.OnMapClickListener{

    private lateinit var overlayFragment: OverlayFragment
    private val viewModel by lazy {ViewModelProviders.of(this).get(HypeMapViewModel::class.java)}
    private lateinit var webView: WebView
    private lateinit var popUp: AlertDialog

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

        overlayFragment = OverlayFragment.newInstance()
        val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.overlay_ui, overlayFragment)
        transaction.commit()

        webView = createWebView()
        popUp = createPopUp()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        viewModel.mMap = googleMap
        viewModel.mMap.uiSettings.isZoomControlsEnabled = true

        addListeners()

        // Set to Toronto
        viewModel.mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(43.6712698,-79.3819235), 1f))

        //startObservers()
    }

    override fun onMarkerClick(p0: Marker?) : Boolean {
        p0?.let { marker ->

            if (marker.tag is PostLocation) {
                viewModel.mMap.setPadding(0, 0, 0, 1100)
                val zoom = viewModel.mMap.cameraPosition.zoom
                var duration = 100f
                if (zoom != 0f) {
                    duration = 300f * (12f / zoom)
                }

                viewModel.mMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(marker.position, 16f),
                    duration.toInt(),
                    object : GoogleMap.CancelableCallback {
                        override fun onCancel() {
                        }

                        override fun onFinish() {
                            marker.tag?.let { tag ->
                                val postLocation: PostLocation = tag as PostLocation
                                postLocation.run {
                                    Log.d(TAG, "Finished animation, Post info: $caption, $userName")
                                    showPopup(postUrl, linkUrl, userName, caption)
                                }
                            }

                        }
                    })
            }
        }
        return true
    }

    override fun onCameraMove() {
    }

    override fun onMapClick(p0: LatLng?) {
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
        val postPopupBuilder = AlertDialog.Builder(this, R.style.CustomAlertDialog)

        postPopupBuilder.setView(webView)

        postPopupBuilder.setNegativeButton(
            "Close")
        { dialog, _ ->
            dialog.dismiss()
        }

        postPopupBuilder.setOnDismissListener {
            viewModel.mMap.setPadding(0, 0, 0, 0)
        }

        return postPopupBuilder.create()
    }

    @Synchronized
    private fun showPopup(postUrl: String, linkUrl: String, userName: String, caption: String) {
        webView.loadUrl(postUrl)
        popUp.setTitle(userName)
        //popUp.setMessage(getTrimmedCaption(caption))

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
            attributes = lp
        }

        popUp.show()
        Log.d(TAG, "Showing popup")
    }

    private fun addListeners() {
        viewModel.mMap.setOnMarkerClickListener(this)
        viewModel.mMap.setOnCameraMoveListener(this)
        viewModel.mMap.setOnMapClickListener(this)
    }
}
