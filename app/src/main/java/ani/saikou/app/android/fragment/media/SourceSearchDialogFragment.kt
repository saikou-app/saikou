package ani.saikou.app.android.fragment.media

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.math.MathUtils.clamp
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import ani.saikou.app.android.adapter.anime.source.AnimeSourceAdapter
import ani.saikou.app.android.adapter.manga.source.MangaSourceAdapter
import ani.saikou.app.android.model.media.MediaDetailsViewModel
import ani.saikou.app.databinding.BottomSheetSourceSearchBinding
import ani.saikou.app.util.BottomSheetDialogFragment
import ani.saikou.app.util.navBarHeight
import ani.saikou.app.util.px
import ani.saikou.core.model.media.Media
import ani.saikou.core.source.anime.AnimeSources
import ani.saikou.core.source.anime.HAnimeSources
import ani.saikou.core.source.manga.MangaSources
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SourceSearchDialogFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetSourceSearchBinding? = null
    private val binding get() = _binding!!
    val model: MediaDetailsViewModel by activityViewModels()
    private var searched = false
    var anime = true
    var i: Int? = null
    var id: Int? = null
    var media: Media? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSourceSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.mediaListContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> { bottomMargin += navBarHeight }

        val scope = viewLifecycleOwner.lifecycleScope
        val imm =
            requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        model.getMedia().observe(viewLifecycleOwner) {
            media = it
            if (media != null) {
                binding.mediaListProgressBar.visibility = View.GONE
                binding.mediaListLayout.visibility = View.VISIBLE

                binding.searchRecyclerView.visibility = View.GONE
                binding.searchProgress.visibility = View.VISIBLE

                i = media!!.selected!!.source
                if (media!!.anime != null) {
                    val source = (if (!media!!.isAdult) AnimeSources else HAnimeSources)[i!!]!!
                    binding.searchSourceTitle.text = source.name
                    binding.searchBarText.setText(media!!.mangaName)
                    fun search() {
                        binding.searchBarText.clearFocus()
                        imm.hideSoftInputFromWindow(binding.searchBarText.windowToken, 0)
                        scope.launch {
                            model.sources.postValue(withContext(Dispatchers.IO) {
                                source.search(
                                    binding.searchBarText.text.toString()
                                )
                            })
                        }
                    }
                    binding.searchBarText.setOnEditorActionListener { _, actionId, _ ->
                        return@setOnEditorActionListener when (actionId) {
                            EditorInfo.IME_ACTION_SEARCH -> {
                                search()
                                true
                            }
                            else -> false
                        }
                    }
                    binding.searchBar.setEndIconOnClickListener { search() }
                    if (!searched) search()

                } else if (media!!.manga != null) {
                    anime = false
                    val source = MangaSources[i!!]!!
                    binding.searchSourceTitle.text = source.name
                    binding.searchBarText.setText(media!!.mangaName)
                    fun search() {
                        binding.searchBarText.clearFocus()
                        imm.hideSoftInputFromWindow(binding.searchBarText.windowToken, 0)
                        scope.launch {
                            model.sources.postValue(withContext(Dispatchers.IO) {
                                source.search(
                                    binding.searchBarText.text.toString()
                                )
                            })
                        }
                    }
                    binding.searchBarText.setOnEditorActionListener { _, actionId, _ ->
                        return@setOnEditorActionListener when (actionId) {
                            EditorInfo.IME_ACTION_SEARCH -> {
                                search()
                                true
                            }
                            else -> false
                        }
                    }
                    binding.searchBar.setEndIconOnClickListener { search() }
                    if (!searched) search()
                }
                searched = true
                model.sources.observe(viewLifecycleOwner) { j ->
                    if (j != null) {
                        binding.searchRecyclerView.visibility = View.VISIBLE
                        binding.searchProgress.visibility = View.GONE
                        binding.searchRecyclerView.adapter =
                            if (anime) AnimeSourceAdapter(j, model, i!!, media!!.id, this, scope)
                            else MangaSourceAdapter(j, model, i!!, media!!.id, this, scope)
                        binding.searchRecyclerView.layoutManager = GridLayoutManager(
                            requireActivity(),
                            clamp(
                                requireActivity().resources.displayMetrics.widthPixels / 124f.px,
                                1,
                                4
                            )
                        )
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun dismiss() {
        model.sources.value = null
        super.dismiss()
    }
}