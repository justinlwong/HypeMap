package justin.apackage.com.hypemap.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.squareup.picasso.Picasso
import justin.apackage.com.hypemap.R
import justin.apackage.com.hypemap.model.MarkerTag
import kotlinx.android.synthetic.main.post_dialog.*

/**
 * A Dialog fragment for showing post
 *
 * @author Justin Wong
 */
class PostDialog: DialogFragment() {

    private lateinit var tag: ArrayList<MarkerTag>

    companion object {
        private const val MARKER_TAG = "marker_tag"

        fun newInstance(tag: ArrayList<MarkerTag>): PostDialog {
            val args = Bundle()
            val newFragment = PostDialog()

            args.putParcelableArrayList(MARKER_TAG, tag)
            newFragment.arguments = args
            return newFragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.PostDialog)

        val bundle = arguments
        if (bundle != null) {
            tag = bundle.getParcelableArrayList(MARKER_TAG)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.post_dialog, container)
    }

    override fun onResume() {
        super.onResume()
        dialog.window?.let { window ->
            window.setGravity(Gravity.BOTTOM)
            val params = window.attributes
            params.width = 1000
            params.height = 1200
            window.attributes = params
            Picasso.with(context)
                .load(tag.first().postUrl)
                .placeholder(R.drawable.image_placeholder)
                .into(postImage)
            postTitle.text = tag.first().userName
            if (tag.first().caption.isNotBlank()) {
                postCaption.text = tag.first().caption
            } else {
                postCaption.visibility = View.GONE
            }
            viewButton.setOnClickListener {
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse(tag.first().linkUrl)
                startActivity(i)
            }

            closeButton.setOnClickListener {
                dismiss()
            }
        }
    }
}