package justin.apackage.com.hypemap.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import justin.apackage.com.hypemap.model.MarkerTag

/**
 * A pager adapter for showing a list of posts
 *
 * @author Justin Wong
 */
class PostCollectionPageAdapter(fm: FragmentManager,
                                private val tags: ArrayList<MarkerTag>) : FragmentPagerAdapter(fm) {

    override fun getItem(position: Int): Fragment {
        return PostFragment.newInstance(tags[position])
    }

    override fun getCount(): Int {
        return tags.size
    }
}