package justin.apackage.com.hypemap

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import com.google.android.gms.maps.CameraUpdateFactory


class OverlayFragment : Fragment() {

    private lateinit var mModel: HypeMapViewModel

    companion object {
        private const val TAG = "OverlayFragment"
        fun newInstance() = OverlayFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mModel = activity?.run {
            ViewModelProviders.of(this).get(HypeMapViewModel::class.java)
        } ?: throw Exception("Invalid Activity")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.overlay_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val getUserEditText: EditText? = getView()?.findViewById(R.id.get_user_input)
        val inflater = LayoutInflater.from(context)
        val footerLayout = inflater.inflate(R.layout.list_footer, null, false)
        val showRecentCheckBox: CheckBox? = footerLayout.findViewById(R.id.show_recent_button)
        val localZoomButton: Button? = getView()?.findViewById(R.id.local_zoom_button)
        val cityZoomButton: Button? = getView()?.findViewById(R.id.city_zoom_button)
        val worldZoomButton: Button? = getView()?.findViewById(R.id.world_zoom_button)
        val menuLaunchButton: FloatingActionButton? = getView()?.findViewById(R.id.menu_launch_button)
        val usersListView: ListView? = getView()?.findViewById(R.id.users_list)

        showRecentCheckBox?.setOnCheckedChangeListener { buttonView, isChecked ->
            val dayInSeconds: Long = 60 * 60 * 24
            if (isChecked) {
                val threeDaysAgoTime = System.currentTimeMillis()/1000 - 3*dayInSeconds
                mModel.filterMarkersByTime(threeDaysAgoTime)
            } else {
                val thirtyDaysAgoTime = System.currentTimeMillis()/1000 - 30*dayInSeconds
                mModel.filterMarkersByTime(thirtyDaysAgoTime)
            }
        }

        getUserEditText?.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val name: String = getUserEditText.text.toString().trim()
                if (name != "") {
                    mModel.addUser(name)
                    Toast.makeText(context, "Fetching user posts...", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Please continue typing", Toast.LENGTH_SHORT).show()
                }
            }
            return@setOnEditorActionListener false
        }

        usersListView?.addFooterView(showRecentCheckBox)

        worldZoomButton?.setOnClickListener {
            mModel.mMap.animateCamera(CameraUpdateFactory.zoomTo(1f))
        }

        cityZoomButton?.setOnClickListener {
            mModel.mMap.animateCamera(CameraUpdateFactory.zoomTo(12f))
        }

        localZoomButton?.setOnClickListener {
            mModel.mMap.animateCamera(CameraUpdateFactory.zoomTo(16f))
        }

        menuLaunchButton?.setOnClickListener {
            usersListView?.run {
                when (visibility) {
                    View.VISIBLE -> {
                        animate()
                            .alpha(0f)
                            .setDuration(500)
                            .setListener(object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator) {
                                    visibility = View.GONE
                                }
                            }).start()
                    }

                    View.GONE -> {
                        visibility = View.VISIBLE
                        alpha = 0f
                        animate()
                            .alpha(1f)
                            .setDuration(500)
                            .setListener(object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator) {
                                    visibility = View.VISIBLE
                                }
                            }).start()
                    }
                }
            }
        }

        mModel.getUsers().observe(this, Observer<List<User>> { usersList ->
            if (usersList != null) {
                val usersListAdapter = UsersListAdapter(
                    this.activity!!,
                    mModel,
                    usersList)
                usersListView?.adapter = usersListAdapter
                if (usersList.isEmpty()) {
                    usersListView?.removeFooterView(showRecentCheckBox)
                } else {
                    if (usersListView?.footerViewsCount == 0) {
                        usersListView.addFooterView(showRecentCheckBox)
                    }
                }
            }
        })
    }
}
