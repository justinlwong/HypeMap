package justin.apackage.com.hypemap.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.ui.IconGenerator
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import justin.apackage.com.hypemap.R
import justin.apackage.com.hypemap.model.HypeMapViewModel
import justin.apackage.com.hypemap.model.PostLocation
import justin.apackage.com.hypemap.model.User
import kotlinx.android.synthetic.main.overlay_fragment.*

/**
 * An overlay fragment which shows list of users and controls on top of map
 *
 * @author Justin Wong
 */
class OverlayFragment : Fragment(), UsersListAdapter.Listener {

    private lateinit var viewModel: HypeMapViewModel
    private lateinit var usersListAdapter: UsersListAdapter
    private val iconFactory by lazy{ IconGenerator(activity!!.applicationContext) }
    private var activeUser: String? = null
    private val addUserPopup: AlertDialog by lazy {createPopUp()}

    companion object {
        private const val TAG = "OverlayFragment"
        fun newInstance() = OverlayFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        iconFactory.setTextAppearance(R.style.TextInfoWindow)
        viewModel = activity?.run {
            ViewModelProviders.of(this).get(HypeMapViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val newInflater = LayoutInflater.from(context)
        return newInflater.inflate(R.layout.overlay_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layoutManager = LinearLayoutManager(context,
            LinearLayoutManager.HORIZONTAL, false)
        usersRecyclerView.layoutManager = layoutManager

        usersListAdapter = UsersListAdapter(
            this.activity!!,
            listOf(),
            this)

        usersRecyclerView.adapter = usersListAdapter

        addUserButton.setOnClickListener {
            addUserPopup.show()
        }

        worldZoomButton?.setOnClickListener {
            zoomTo(1f)
        }

        cityZoomButton?.setOnClickListener {
            zoomTo(12f)
        }

        localZoomButton?.setOnClickListener {
            zoomTo(16f)
        }

        startObservers()
    }

    override fun onActiveUserUpdate(userName: String) {
        showOnlyUserPosts(userName)
    }

    private fun showOnlyUserPosts(userName: String) {
        activeUser = userName
        viewModel.mMap.clear()
        Schedulers.io().scheduleDirect {
            val posts = viewModel.getPostLocationsBlocking()
            posts?.forEach { post ->
                if (post.userName == activeUser) {
                    AndroidSchedulers.mainThread().scheduleDirect {
                        addMarkerAtLocation(
                            LatLng(post.latitude, post.longitude),
                            post.locationName,
                            post
                        )
                    }
                }
            }
        }
    }

    private fun startObservers() {
        viewModel.getUsers().observe(this, Observer<List<User>> { usersList ->
            if (usersList != null) {
                usersListAdapter.setItems(usersList)
            }
        })

        viewModel.getPostLocations().observe(this, Observer { posts ->
            viewModel.mMap.clear()
            if (activeUser != null) {
                posts?.let {
                    it.filter { post -> post.userName == activeUser }.forEach { post ->
                        addMarkerAtLocation(
                            LatLng(post.latitude, post.longitude),
                            post.locationName,
                            post
                        )
                    }
                }
            }
        })
    }

    private fun addMarkerAtLocation(location: LatLng, locationName: String, postLocationData: PostLocation) {
        val baseMarkerOptions = MarkerOptions().position(location)

        val mkr = viewModel.mMap.addMarker(baseMarkerOptions)
        mkr.tag = postLocationData


        val infoMkr = viewModel.mMap.addMarker(baseMarkerOptions.anchor(0.5f, 2.25f))
        infoMkr.setIcon(BitmapDescriptorFactory.fromBitmap(iconFactory.makeIcon(locationName)))
        infoMkr.tag = locationName
    }

    private fun zoomTo(zoomLevel: Float) {
        viewModel.mMap.animateCamera(CameraUpdateFactory.zoomTo(zoomLevel))
    }

    private fun createPopUp(): AlertDialog {
        val popupBuilder = AlertDialog.Builder(context, R.style.CustomAlertDialog)
        val editText = EditText(context)
        val container = FrameLayout(context)
        val params = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT)
        params.marginStart = 20
        params.marginEnd = 20
        editText.layoutParams = params
        container.removeAllViews()
        container.addView(editText)

        popupBuilder.setView(container)
            .setNegativeButton("Close")
                { dialog, _ ->
                    dialog.dismiss()
                }
            .setPositiveButton("OK")
                { dialog, _ ->
                    viewModel.addUser(editText.text.toString())
                    dialog.dismiss()
                }
            .setOnDismissListener {
                viewModel.mMap.setPadding(0, 0, 0, 0)
            }
            .setTitle("Add a user to follow")

        return popupBuilder.create()
    }
}
