package ani.saikou.user

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import ani.saikou.R
import ani.saikou.Refresh
import ani.saikou.databinding.ActivityListBinding
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityListBinding
    private val scope = lifecycleScope

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = ContextCompat.getColor(this, R.color.nav_bg)
        val anime = intent.getBooleanExtra("anime", true)
        binding.listTitle.text = intent.getStringExtra("username") + "'s " + (if (anime) "Anime" else "Manga") + " List"

        val model: ListViewModel by viewModels()
        model.getLists().observe(this) {
            if (it != null) {
                binding.listProgressBar.visibility = View.GONE
                binding.listViewPager.adapter = ListViewPagerAdapter(it.size, this)
                val keys = it.keys.toList()
                val values = it.values.toList()
                TabLayoutMediator(binding.listTabLayout, binding.listViewPager) { tab, position ->
                    tab.text = "${keys[position]} (${values[position].size})"
                }.attach()
            }
        }
        val live = Refresh.activity.getOrPut(this.hashCode()) { MutableLiveData(true) }
        live.observe(this) {
            if (it) {
                scope.launch {
                    withContext(Dispatchers.IO) { model.loadLists(anime, intent.getIntExtra("userId", 0)) }
                    live.postValue(false)
                }
            }
        }

       binding.listSort.setOnClickListener {
           val popup = PopupMenu(this, it)
           popup.setOnMenuItemClickListener { item ->
               val sort = when (item.itemId) {
                   R.id.score -> "score"
                   R.id.title -> "title"
                   R.id.updated -> "updatedAt"
                   else -> null
               }

               binding.listProgressBar.visibility = View.VISIBLE
               binding.listViewPager.adapter = null
               scope.launch {
                   withContext(Dispatchers.IO) { model.loadLists(anime, intent.getIntExtra("userId", 0), sort) }
               }
               true
           }
           popup.inflate(R.menu.list_sort_menu)
           popup.show()
       }
    }
}