package justin.apackage.com.hypemap

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.squareup.picasso.Picasso

class UsersListAdapter(private val context: Context,
                       private val inputSource: List<User>) : BaseAdapter() {
    private val inflater: LayoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

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
        val userView = inflater.inflate(R.layout.users_list_item, parent, false)
        val user = inputSource[position]
        val nameTextView: TextView = userView.findViewById(R.id.user_name)
        val profileImageView: ImageView = userView.findViewById(R.id.user_image)

        nameTextView.text = user.userName

        Picasso.with(context).load(user.profilePicUrl).placeholder(R.mipmap.ic_launcher).into(profileImageView)

        return userView
    }


}