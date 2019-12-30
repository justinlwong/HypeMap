package justin.apackage.com.hypemap.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.maps.CameraUpdateFactory
import justin.apackage.com.hypemap.R
import justin.apackage.com.hypemap.model.HypeMapViewModel
import justin.apackage.com.hypemap.model.User
import kotlinx.android.synthetic.main.overlay_fragment.*

/**
 * An overlay fragment which shows list of users and controls on top of map
 *
 * @author Justin Wong
 */
class OverlayFragment : Fragment() {

    private lateinit var mModel: HypeMapViewModel
    private lateinit var usersListAdapter: UsersListAdapter

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
        return newInflater.inflate(R.layout.overlay_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layoutManager = LinearLayoutManager(context,
            LinearLayoutManager.HORIZONTAL, false)
        usersRecyclerView.layoutManager = layoutManager

        usersListAdapter = UsersListAdapter(
            this.activity!!,
            mModel,
            listOf())

        usersRecyclerView.adapter = usersListAdapter

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

    private fun startObservers() {
        mModel.getUsers().observe(this, Observer<List<User>> { usersList ->
            if (usersList != null) {
                usersListAdapter.setItems(usersList)
            }
        })
    }

    private fun zoomTo(zoomLevel: Float) {
        mModel.mMap.animateCamera(CameraUpdateFactory.zoomTo(zoomLevel))
    }
}
