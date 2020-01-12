package justin.apackage.com.hypemap.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import justin.apackage.com.hypemap.R
import justin.apackage.com.hypemap.model.MarkerTag
import kotlinx.android.synthetic.main.post_dialog.*

/**
 * A Dialog fragment for showing list of posts at a location
 *
 * @author Justin Wong
 */
class PostContainerDialog: DialogFragment() {

    private lateinit var tags: ArrayList<MarkerTag>
    private lateinit var pagerAdapter: PostCollectionPageAdapter

    companion object {
        private const val MARKER_TAG = "marker_tag"

        fun newInstance(tag: ArrayList<MarkerTag>): PostContainerDialog {
            val args = Bundle()
            val newFragment = PostContainerDialog()

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
            tags = bundle.getParcelableArrayList(MARKER_TAG)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.post_dialog, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        childFragmentManager.let {
            pagerAdapter = PostCollectionPageAdapter(it, tags)
            val pager: ViewPager = view.findViewById(R.id.viewPager)
            val tabLayout: TabLayout = view.findViewById(R.id.tabLayout)
            pager.adapter = pagerAdapter
            if (tags.size > 1) {
                tabLayout.visibility = View.VISIBLE
                tabLayout.setupWithViewPager(pager)
            } else {
                tabLayout.visibility = View.GONE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        dialog.window?.let { window ->
            window.setGravity(Gravity.BOTTOM)
            val params = window.attributes
            params.width = 1000
            params.height = 1200
            window.attributes = params
            viewButton.setOnClickListener {
                val i = Intent(Intent.ACTION_VIEW)
                val curTag = tags[viewPager.currentItem]

                i.data = Uri.parse(curTag.linkUrl)
                startActivity(i)
            }

            closeButton.setOnClickListener {
                dismiss()
            }
        }
    }
}