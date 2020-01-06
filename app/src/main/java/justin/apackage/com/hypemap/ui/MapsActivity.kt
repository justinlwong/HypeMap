package justin.apackage.com.hypemap.ui

import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.work.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import justin.apackage.com.hypemap.R
import justin.apackage.com.hypemap.model.HypeMapViewModel
import justin.apackage.com.hypemap.model.PostLocation
import justin.apackage.com.hypemap.worker.UpdatePostsWorker
import java.util.concurrent.TimeUnit

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
    private lateinit var postDialog: PostDialog
    private var curInfoStatus: Boolean = false

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
        postDialog = PostDialog()

        // Start update work
        startUpdateWork()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        viewModel.mMap = googleMap
        viewModel.mMap.uiSettings.isZoomControlsEnabled = true

        addListeners()

        // Set to Toronto
        viewModel.mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(43.6712698,-79.3819235), 1f))
    }

    override fun onMarkerClick(p0: Marker?) : Boolean {
        p0?.let { marker ->
            if (marker.tag is PostLocation) {
                val post = marker.tag as PostLocation
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
                            Log.d(TAG, "Interrupted")
                        }

                        override fun onFinish() {
                            val infoMarker = viewModel.getInfoMarkersMap()[post.id]
                            if (infoMarker != null) {
                                infoMarker.isVisible = true
                            }
                            val postDialog = PostDialog.newInstance(post)
                            postDialog.show(supportFragmentManager, "postDialog")
                        }
                    })
                return true
            }
        }
        return false
    }

    override fun onCameraMove() {
    }

    override fun onMapClick(p0: LatLng?) {
        showInfoMarkers(!curInfoStatus)
    }

    private fun showInfoMarkers(showStatus: Boolean) {
        curInfoStatus = showStatus
        for (( _, marker) in viewModel.getInfoMarkersMap()) {
            marker.isVisible = curInfoStatus
        }
    }

    private fun addListeners() {
        viewModel.mMap.setOnMarkerClickListener(this)
        viewModel.mMap.setOnCameraMoveListener(this)
        viewModel.mMap.setOnMapClickListener(this)
    }

    private fun startUpdateWork() {
        val work = createWorkRequest(Data.EMPTY)
        WorkManager.getInstance().enqueueUniquePeriodicWork("Smart work", ExistingPeriodicWorkPolicy.KEEP, work)

        // Observe the result od the work
        WorkManager.getInstance().getWorkInfoByIdLiveData(work.id)
            .observe(this, Observer { workInfo ->
                if (workInfo != null && workInfo.state == WorkInfo.State.SUCCEEDED) {
                    Log.d(TAG, "Finished update work successfully")
                }
            })
    }

    private fun createConstraints() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.UNMETERED)  // if connected to WIFI
        .setRequiresBatteryNotLow(true)                 // if the battery is not low
        .setRequiresStorageNotLow(true)                 // if the storage is not low
        .build()

    private fun createWorkRequest(data: Data) = PeriodicWorkRequest.Builder(
        UpdatePostsWorker::class.java, 3, TimeUnit.HOURS)
        .setInputData(data)
        .setConstraints(createConstraints())
        .setBackoffCriteria(BackoffPolicy.LINEAR, PeriodicWorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
        .build()
}
