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
        val eView: EditText? = getView()?.findViewById(R.id.get_user_input)
        val cButton = Button(context)
        val zOutButton: Button? = getView()?.findViewById(R.id.zoom_out_button)
        val usersListView: ListView? = getView()?.findViewById(R.id.users_list)

        cButton.text = "Clear"
        cButton.setTextColor(ResourcesCompat.getColor(context!!.resources, R.color.grey, null))
        cButton.setBackgroundColor(ResourcesCompat.getColor(context!!.resources, R.color.white, null))

        eView?.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                mModel.addUser(eView.text.toString())
                Toast.makeText(context, "Fetching user posts...", Toast.LENGTH_LONG).show()
            }
            return@setOnEditorActionListener false
        }

        usersListView?.addFooterView(cButton)

        zOutButton?.setOnClickListener {
            mModel.mMap.animateCamera(CameraUpdateFactory.zoomBy(-14.0f))
        }

        mModel.getUsers().observe(this, Observer<List<User>> { usersList ->
            if (usersList != null) {
                val usersListAdapter = UsersListAdapter(
                    this.activity!!,
                    mModel,
                    usersList)
                usersListView?.adapter = usersListAdapter
                if (usersList.isEmpty()) {
                    usersListView?.removeFooterView(cButton)
                } else {
                    if (usersListView?.footerViewsCount == 0) {
                        cButton.setOnClickListener {
                            mModel.removeAll()
                        }
                        usersListView.addFooterView(cButton)
                    }
                }
            }
        })
    }
}
