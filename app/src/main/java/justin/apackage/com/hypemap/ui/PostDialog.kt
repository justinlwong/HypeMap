package justin.apackage.com.hypemap.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.squareup.picasso.Picasso
import justin.apackage.com.hypemap.R
import justin.apackage.com.hypemap.model.PostLocation
import kotlinx.android.synthetic.main.post_dialog.*
import android.view.Gravity

/**
 * A Dialog fragment for showing post
 *
 * @author Justin Wong
 */
class PostDialog: DialogFragment() {

    private lateinit var postLocation: PostLocation

    companion object {
        private const val POST_LOCATION = "post_location"

        fun newInstance(postLocation: PostLocation): PostDialog {
            val args = Bundle()
            val newFragment = PostDialog()

            args.putParcelable(POST_LOCATION, postLocation)
            newFragment.arguments = args
            return newFragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.PostDialog)

        val bundle = arguments
        if (bundle != null) {
            postLocation = bundle.getParcelable(POST_LOCATION)
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
            Picasso.with(context).load(postLocation.postUrl).into(postImage)
            postTitle.text = postLocation.userName
            if (postLocation.caption.isNotBlank()) {
                postCaption.text = postLocation.caption
            } else {
                postCaption.visibility = View.GONE
            }
            viewButton.setOnClickListener {
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse(postLocation.linkUrl)
                startActivity(i)
            }

            closeButton.setOnClickListener {
                dismiss()
            }
        }
    }
}