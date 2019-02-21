package justin.apackage.com.hypemap

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.res.ResourcesCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
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
        val clearButton = Button(context)
        val cityZoomButton: Button? = getView()?.findViewById(R.id.city_zoom_button);
        val worldZoomButton: Button? = getView()?.findViewById(R.id.world_zoom_button)
        val usersListView: ListView? = getView()?.findViewById(R.id.users_list)

        clearButton.text = "Clear"
        clearButton.setTextColor(ResourcesCompat.getColor(context!!.resources, R.color.grey, null))
        clearButton.setBackgroundColor(ResourcesCompat.getColor(context!!.resources, R.color.white, null))
        clearButton.setOnClickListener {
            mModel.removeAll()
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

        usersListView?.addFooterView(clearButton)

        worldZoomButton?.setOnClickListener {
            mModel.mMap.animateCamera(CameraUpdateFactory.zoomTo(1f))
        }

        cityZoomButton?.setOnClickListener {
            mModel.mMap.animateCamera(CameraUpdateFactory.zoomTo(12f))
        }

        mModel.getUsers().observe(this, Observer<List<User>> { usersList ->
            if (usersList != null) {
                val usersListAdapter = UsersListAdapter(
                    this.activity!!,
                    mModel,
                    usersList)
                usersListView?.adapter = usersListAdapter
                if (usersList.isEmpty()) {
                    usersListView?.removeFooterView(clearButton)
                } else {
                    if (usersListView?.footerViewsCount == 0) {
                        clearButton.setOnClickListener {
                            mModel.removeAll()
                        }
                        usersListView.addFooterView(clearButton)
                    }
                }
            }
        })
    }
}
