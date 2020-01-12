package justin.apackage.com.hypemap.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.squareup.picasso.Picasso
import justin.apackage.com.hypemap.R
import justin.apackage.com.hypemap.model.MarkerTag
import kotlinx.android.synthetic.main.post_layout.*
import java.text.SimpleDateFormat
import java.util.*

class PostFragment: Fragment() {

    private lateinit var tag: MarkerTag

    companion object {
        private const val MARKER_TAG = "marker_tag"

        fun newInstance(tag: MarkerTag): PostFragment {
            val args = Bundle()
            val newFragment = PostFragment()

            args.putParcelable(MARKER_TAG, tag)
            newFragment.arguments = args
            return newFragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bundle = arguments
        if (bundle != null) {
            tag = bundle.getParcelable(MARKER_TAG)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.post_layout, container, false)
    }

    override fun onResume() {
        super.onResume()
        Picasso.with(context)
            .load(tag.postUrl)
            .placeholder(R.drawable.image_placeholder)
            .into(postImage)
        postTitle.text = tag.userName
        val formatter = SimpleDateFormat("MMMM d, yyyy", Locale.CANADA)
        postDate.text = formatter.format(Date(tag.timestamp * 1000L))
        if (tag.caption.isNotBlank()) {
            postCaption.text = tag.caption
        } else {
            postCaption.visibility = View.GONE
        }
    }
}