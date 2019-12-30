package justin.apackage.com.hypemap.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import justin.apackage.com.hypemap.R
import justin.apackage.com.hypemap.model.HypeMapViewModel
import justin.apackage.com.hypemap.model.User
import kotlinx.android.synthetic.main.users_list_item.view.*

class UsersListAdapter(private val context: Context,
                       private val viewModel: HypeMapViewModel,
                       private var users: List<User>) : RecyclerView.Adapter<UsersListAdapter.ViewHolder>() {

    class ViewHolder (view: View) : RecyclerView.ViewHolder(view) {
        val userName: TextView = view.userName
        val userImage: ImageView = view.userImage
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.users_list_item,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return users.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        var name = users[position].userName
        val picUrl = users[position].profilePicUrl
        if (name.length > 7) {
            name = "${name.substring(0, 7)}.."
        }
        holder.userName.text = name
        Picasso.with(context).load(picUrl).placeholder(R.mipmap.ic_launcher).into(holder.userImage)
    }

    fun setItems(users: List<User>) {
        this.users = users
        notifyDataSetChanged()
    }

    companion object {
        private const val TAG = "UsersListAdapter"
    }
}
