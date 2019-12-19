package justin.apackage.com.hypemap.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import com.google.android.gms.maps.CameraUpdateFactory
import justin.apackage.com.hypemap.model.HypeMapViewModel
import justin.apackage.com.hypemap.R
import justin.apackage.com.hypemap.model.User
import kotlinx.android.synthetic.main.overlay_fragment.*

/**
 * An overlay fragment which shows list of users and controls on top of map
 *
 * @author Justin Wong
 */
class OverlayFragment : Fragment() {

    private lateinit var mModel: HypeMapViewModel
    private lateinit var footerView: View

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

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val newInflater = LayoutInflater.from(context)
        footerView = newInflater.inflate(R.layout.list_footer, null, false)
        return newInflater.inflate(R.layout.overlay_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val showRecentCheckBox: CheckBox? = footerView.findViewById(R.id.showRecentCheckBox)

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

        usersListView?.addFooterView(footerView)

        worldZoomButton?.setOnClickListener {
            zoomTo(1f)
        }

        cityZoomButton?.setOnClickListener {
            zoomTo(12f)
        }

        localZoomButton?.setOnClickListener {
            zoomTo(16f)
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
                    usersList
                )
                usersListView?.adapter = usersListAdapter
                if (usersList.isEmpty()) {
                    usersListView?.removeFooterView(footerView)
                } else {
                    if (usersListView?.footerViewsCount == 0) {
                        usersListView.addFooterView(footerView)
                    }
                }
            }
        })
    }

    private fun zoomTo(zoomLevel: Float) {
        mModel.mMap.animateCamera(CameraUpdateFactory.zoomTo(zoomLevel))
    }
}
