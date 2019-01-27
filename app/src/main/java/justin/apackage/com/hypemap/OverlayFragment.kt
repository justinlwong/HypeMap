package justin.apackage.com.hypemap

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import com.google.android.gms.maps.CameraUpdateFactory


class OverlayFragment : Fragment() {

    private lateinit var mModel: HypeMapViewModel

    companion object {
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
        val eView: EditText? = getView()?.findViewById(R.id.get_user_input)
        val cButton: Button? = getView()?.findViewById(R.id.clear_button)
        val zInButton: Button? = getView()?.findViewById(R.id.zoom_in_button)
        val zOutButton: Button? = getView()?.findViewById(R.id.zoom_out_button)
        val usersListView: ListView? = getView()?.findViewById(R.id.users_list)

        eView?.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                mModel.addUser(eView.text.toString())
            }
            return@setOnEditorActionListener false
        }

        cButton?.setOnClickListener {
            mModel.removeAll()
        }

        zInButton?.setOnClickListener {
            mModel.mMap.animateCamera(CameraUpdateFactory.zoomBy(3.0f))
        }

        zOutButton?.setOnClickListener {
            mModel.mMap.animateCamera(CameraUpdateFactory.zoomBy(-14.0f))
        }

        mModel.getUsers().observe(this, Observer<List<User>> { usersList ->
            if (usersList != null) {
                val usersListAdapter = UsersListAdapter(
                    this.activity!!,
                    usersList)
                usersListView?.adapter = usersListAdapter
            }
        })
    }
}
