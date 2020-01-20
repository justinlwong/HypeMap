package justin.apackage.com.hypemap.ui

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
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
import justin.apackage.com.hypemap.model.MarkerTag
import justin.apackage.com.hypemap.model.User
import kotlinx.android.synthetic.main.overlay_fragment.*
import java.util.concurrent.TimeUnit

/**
 * An overlay fragment which shows list of users and controls on top of map
 *
 * @author Justin Wong
 */
class OverlayFragment : Fragment(), UsersListAdapter.Listener {

    private lateinit var viewModel: HypeMapViewModel
    private lateinit var usersListAdapter: UsersListAdapter
    private val iconFactory by lazy{ IconGenerator(activity!!.applicationContext) }
    private var activeUserId: String? = null
    private val addUserPopup: AlertDialog by lazy { createPopUp() }

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

    override fun onStart() {
        super.onStart()
        activeUserId?.let {
            showOnlyUserPosts(it)
        }
    }

    override fun onActiveUserUpdate(userId: String) {
        showOnlyUserPosts(userId)
    }

    override fun onRemoveUser(user: User) {
        viewModel.deleteUser(user)
    }

    private fun showOnlyUserPosts(userId: String) {
        activeUserId = userId
        clearMarkers()
        Schedulers.io().scheduleDirect {
            val posts = viewModel.getPostLocationsBlocking()
            posts?.forEach { post ->
                if (post.userId == activeUserId) {
                    val location = viewModel.getLocation(post.locationId)
                    val user = viewModel.getUser(post.userId)
                    AndroidSchedulers.mainThread().scheduleDirect {
                        if (user != null && location != null) {
                            val tag = MarkerTag(post.id,
                                user.userName,
                                location.locationId,
                                post.locationName,
                                post.postUrl,
                                post.linkUrl,
                                post.caption,
                                post.timestamp)

                            addMarkerAtLocation(
                                location.locationId,
                                LatLng(location.latitude, location.longitude),
                                tag)
                        }
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

        viewModel.getPosts().observe(this, Observer { posts ->
            clearMarkers()
            if (activeUserId != null) {
                Schedulers.io().scheduleDirect {
                    posts?.let {
                        it.filter { post -> post.userId == activeUserId }.forEach { post ->
                            val location = viewModel.getLocation(post.locationId)
                            val user = viewModel.getUser(post.userId)
                            AndroidSchedulers.mainThread().scheduleDirect {
                                if (user != null && location != null) {
                                    val tag = MarkerTag(post.id,
                                        user.userName,
                                        location.locationId,
                                        post.locationName,
                                        post.postUrl,
                                        post.linkUrl,
                                        post.caption,
                                        post.timestamp)

                                    addMarkerAtLocation(
                                        location.locationId,
                                        LatLng(location.latitude, location.longitude),
                                        tag)
                                }
                            }
                        }
                    }
                }
            }
        })
    }

    private fun addMarkerAtLocation(locationId: String, location: LatLng, tag: MarkerTag) {
        val baseMarkerOptions = MarkerOptions().position(location)
        val existingMarker = viewModel.getMainMarkersMap()[locationId]
        if (existingMarker == null) {
            val twoDaysAgo: Long = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) - TimeUnit.DAYS.toSeconds(1)
            var mainOptions = baseMarkerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            if (tag.timestamp > twoDaysAgo) {
                mainOptions = baseMarkerOptions.icon(
                    BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)
                )
            }
            val mainMarker = viewModel.mMap.addMarker(mainOptions)
            mainMarker.tag = listOf(tag)
            viewModel.getMainMarkersMap()[locationId] = mainMarker
        } else {
            val tagList = (existingMarker.tag as? MutableList<*>)?.filterIsInstance<MarkerTag>()
            tagList?.let {
                val setIds: MutableSet<String> = mutableSetOf()
                tagList.forEach {
                    setIds.add(it.id)
                }
                if (!setIds.contains(tag.id)) {
                    val updatedList = tagList.toMutableList()
                    updatedList.add(tag)
                    existingMarker.tag = updatedList
                }
            }
        }

        if (viewModel.getInfoMarkersMap()[locationId] == null) {
            val infoOptions = baseMarkerOptions.anchor(0.5f, 2.25f)
                .icon(BitmapDescriptorFactory.fromBitmap(iconFactory.makeIcon(tag.locationName)))
            val infoMkr = viewModel.mMap.addMarker(infoOptions)
            infoMkr.tag = tag.locationName
            viewModel.getInfoMarkersMap()[locationId] = infoMkr
        }
    }

    private fun zoomTo(zoomLevel: Float) {
        viewModel.mMap.animateCamera(CameraUpdateFactory.zoomTo(zoomLevel))
    }

    private fun createPopUp(): AlertDialog {
        val popupBuilder = AlertDialog.Builder(context, R.style.BasicDialog)
        val editText = EditText(context)
        val container = FrameLayout(context)
        val params = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT)
        params.marginStart = 20
        params.marginEnd = 20
        editText.layoutParams = params
        editText.setSingleLine()
        editText.imeOptions = EditorInfo.IME_ACTION_DONE
        editText.setOnKeyListener { v, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                addUser(editText)
                return@setOnKeyListener true
            }
            false
        }
        editText.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                addUserPopup.getButton(DialogInterface.BUTTON_POSITIVE).isEnabled = s?.trim().toString() != ""
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
        })
        container.removeAllViews()
        container.addView(editText)

        popupBuilder.setView(container)
            .setNegativeButton("Close")
                { dialog, _ ->
                    dialog.dismiss()
                }
            .setPositiveButton("OK")
                { _, _ ->
                    addUser(editText)
                }
            .setOnDismissListener {
                viewModel.mMap.setPadding(0, 0, 0, 0)
            }
            .setTitle("Add a user to follow")

        return popupBuilder.create()
    }

    private fun addUser(editText: EditText) {
        Toast.makeText(context, "Fetching user...", Toast.LENGTH_SHORT).show()
        viewModel.addUser(editText.text.toString())
        addUserPopup.dismiss()
    }

    private fun clearMarkers() {
        viewModel.mMap.clear()
        viewModel.getInfoMarkersMap().clear()
        viewModel.getMainMarkersMap().clear()
    }
}
