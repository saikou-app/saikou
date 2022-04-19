package ani.saikou.app.android.adapter.user

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import ani.saikou.app.android.fragment.user.ListFragment

class ListViewPagerAdapter(private val size: Int, fragment: FragmentActivity) :
    FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = size
    override fun createFragment(position: Int): Fragment = ListFragment.newInstance(position)
}