package justin.apackage.com.hypemap.ui

import android.content.Context
import androidx.core.content.res.ResourcesCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.squareup.picasso.Picasso
import justin.apackage.com.hypemap.model.HypeMapViewModel
import justin.apackage.com.hypemap.R
import justin.apackage.com.hypemap.model.User

class UsersListAdapter(private val context: Context,
                       private val viewModel: HypeMapViewModel,
                       private val inputSource: List<User>) : BaseAdapter() {
    private val inflater: LayoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    companion object {
        private const val TAG = "UsersListAdapter"
    }

    override fun getItem(position: Int): Any {
        return inputSource[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return inputSource.size
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val userView: View = convertView ?: inflater.inflate(R.layout.users_list_item, parent, false)
        val user = inputSource[position]
        val nameTextView: TextView = userView.findViewById(R.id.userName)
        val profileImageView: ImageView = userView.findViewById(R.id.userImage)

        var name = user.userName
        if (name.length > 7) {
            name = "${name.substring(0, 7)}.."
        }
        nameTextView.text = name

        Picasso.with(context).load(user.profilePicUrl).placeholder(R.mipmap.ic_launcher).into(profileImageView)

        if (user.visible) {
            userView.setBackgroundColor(ResourcesCompat.getColor(context.resources,
                R.color.white, null))
            userView.alpha = 1f
        } else {
            userView.setBackgroundColor(ResourcesCompat.getColor(context.resources,
                R.color.grey, null))
            userView.alpha = 0.75f
        }

        userView.setOnClickListener {
            Log.d(TAG, "${user.userName} clicked at visibility: ${user.visible}")
            viewModel.showUserMarkers(user.userName, !user.visible)
        }

        userView.setOnLongClickListener {
            viewModel.removeUser(user.userName)
            return@setOnLongClickListener true
        }

        return userView
    }
}