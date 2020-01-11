package justin.apackage.com.hypemap.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import justin.apackage.com.hypemap.R
import justin.apackage.com.hypemap.model.User
import kotlinx.android.synthetic.main.users_list_item.view.*

class UsersListAdapter(private val context: Context,
                       private var users: List<User>,
                       private val listener: Listener) : RecyclerView.Adapter<UsersListAdapter.ViewHolder>() {

    interface Listener {
        fun onActiveUserUpdate(userId: String)
    }

    private var activePosition: Int = 0

    class ViewHolder (view: View) : RecyclerView.ViewHolder(view) {
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
        val picUrl = users[position].profilePicUrl
        Picasso.with(context).load(picUrl).placeholder(R.mipmap.ic_launcher).into(holder.userImage)
        if (activePosition != position) {
            holder.userImage.background = null
        } else {
            holder.userImage.background = context.getDrawable(R.drawable.user_back)
        }
        holder.userImage.setOnClickListener {
            activePosition = position
            listener.onActiveUserUpdate(users[position].userId)
            notifyDataSetChanged()
        }
    }

    fun setItems(users: List<User>) {
        val oldUsers = this.users
        if (users.isNotEmpty()) {
            if (oldUsers.isNotEmpty()) {
                activePosition = users.size - 1
            }
            listener.onActiveUserUpdate(users[activePosition].userId)
        }
        this.users = users
        notifyDataSetChanged()
    }

    companion object {
        private const val TAG = "UsersListAdapter"
    }
}
