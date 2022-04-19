package ani.saikou.app.android.adapter.media

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import ani.saikou.app.R
import ani.saikou.app.android.activity.media.SearchActivity
import ani.saikou.app.util.anilist.anilist.Anilist
import ani.saikou.app.databinding.ItemSearchHeaderBinding
import ani.saikou.core.service.STORE


class SearchAdapter(private val activity: SearchActivity) :
    RecyclerView.Adapter<SearchAdapter.SearchHeaderViewHolder>() {
    private val itemViewType = 6969
    lateinit var search: Runnable
    lateinit var requestFocus: Runnable
    private var textWatcher: TextWatcher? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchHeaderViewHolder {
        val binding =
            ItemSearchHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SearchHeaderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SearchHeaderViewHolder, position: Int) {
        val binding = holder.binding

        binding.searchBar.hint = activity.type
        val imm: InputMethodManager =
            activity.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager

        when (activity.style) {
            0 -> {
                binding.searchResultGrid.alpha = 1f
                binding.searchResultList.alpha = 0.33f
            }
            1 -> {
                binding.searchResultList.alpha = 1f
                binding.searchResultGrid.alpha = 0.33f
            }
        }

        var adult = activity.adult
        var listOnly = activity.listOnly

        binding.searchBarText.removeTextChangedListener(textWatcher)
        binding.searchBarText.setText(activity.searchText)

        binding.searchAdultCheck.isChecked = adult
        binding.searchList.isChecked = listOnly == true

        binding.searchGenre.setText(activity.genre)
        binding.searchGenre.setAdapter(
            ArrayAdapter(
                binding.root.context,
                R.layout.item_dropdown,
                Anilist.genres ?: STORE.loadData<List<String>>("genres_list") ?: listOf()
            )
        )
        binding.searchSortBy.setText(activity.sortBy)
        binding.searchSortBy.setAdapter(
            ArrayAdapter(
                binding.root.context,
                R.layout.item_dropdown,
                Anilist.sortBy.keys.toTypedArray()
            )
        )
        binding.searchTag.setText(activity.tag)
        binding.searchTag.setAdapter(
            ArrayAdapter(
                binding.root.context,
                R.layout.item_dropdown,
                Anilist.tags ?: STORE.loadData<ArrayList<String>>("tags_list") ?: arrayListOf()
            )
        )

        fun searchTitle() {
            val search =
                if (binding.searchBarText.text.toString() != "") binding.searchBarText.text.toString() else null
            val genre =
                if (binding.searchGenre.text.toString() != "") binding.searchGenre.text.toString() else null
            val sortBy =
                if (binding.searchSortBy.text.toString() != "") binding.searchSortBy.text.toString() else null
            val tag =
                if (binding.searchTag.text.toString() != "") binding.searchTag.text.toString() else null
            activity.search(search, genre, tag, sortBy, adult, listOnly)
        }

        textWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                searchTitle()
            }
        }
        binding.searchBarText.addTextChangedListener(textWatcher)

        binding.searchBarText.setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_SEARCH -> {
                    searchTitle()
                    binding.searchBarText.clearFocus()
                    imm.hideSoftInputFromWindow(binding.searchBarText.windowToken, 0)
                    true
                }
                else -> false
            }
        }
        binding.searchBar.setEndIconOnClickListener { searchTitle() }
        binding.searchGenre.setOnItemClickListener { _, _, _, _ -> searchTitle() }
        binding.searchTag.setOnItemClickListener { _, _, _, _ -> searchTitle() }
        binding.searchSortBy.setOnItemClickListener { _, _, _, _ -> searchTitle() }

        binding.searchClear.setOnClickListener {
            binding.searchGenre.setText("")
            binding.searchTag.setText("")
            binding.searchSortBy.setText("")
            searchTitle()
        }

        binding.searchResultGrid.setOnClickListener {
            it.alpha = 1f
            binding.searchResultList.alpha = 0.33f
            activity.style = 0
            STORE.saveData("searchStyle", 0)
            activity.recycler()
        }
        binding.searchResultList.setOnClickListener {
            it.alpha = 1f
            binding.searchResultGrid.alpha = 0.33f
            activity.style = 1
            STORE.saveData("searchStyle", 1)
            activity.recycler()
        }

        if (Anilist.adult) {
            binding.searchAdultCheck.visibility = View.VISIBLE
            binding.searchAdultCheck.isChecked = adult
            if (adult) {
                binding.searchGenreCont.visibility = View.GONE
                binding.searchTagCont.visibility = View.VISIBLE
            }
            binding.searchAdultCheck.setOnCheckedChangeListener { _, b ->
                adult = b
                if (b && Anilist.tags != null) {
                    binding.searchGenreCont.visibility = View.GONE
                    binding.searchTagCont.visibility = View.VISIBLE
                } else {
                    binding.searchGenreCont.visibility = View.VISIBLE
                    binding.searchTagCont.visibility = View.GONE
                }
                binding.searchGenre.setText("")
                binding.searchTag.setText("")
                searchTitle()
            }
        } else {
            binding.searchAdultCheck.visibility = View.GONE
        }
        binding.searchList.setOnCheckedChangeListener { _, b ->
            listOnly = if (b) true else null
            searchTitle()
        }

        search = Runnable { searchTitle() }
        requestFocus = Runnable { binding.searchBarText.requestFocus() }
    }


    override fun getItemCount(): Int = 1

    inner class SearchHeaderViewHolder(val binding: ItemSearchHeaderBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int {
        return itemViewType
    }
}