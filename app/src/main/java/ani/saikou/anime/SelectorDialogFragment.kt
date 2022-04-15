package ani.saikou.anime

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ani.saikou.*
import ani.saikou.databinding.BottomSheetSelectorBinding
import ani.saikou.databinding.ItemStreamBinding
import ani.saikou.databinding.ItemUrlBinding
import ani.saikou.media.Media
import ani.saikou.media.MediaDetailsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat

class SelectorDialogFragment : BottomSheetDialogFragment() {
    private var _binding: BottomSheetSelectorBinding? = null
    private val binding get() = _binding!!
    val model: MediaDetailsViewModel by activityViewModels()
    private var scope: CoroutineScope = lifecycleScope
    private var media: Media? = null
    private var episode: Episode? = null
    private var prevEpisode: String? = null
    private var makeDefault = false
    private var selected: String? = null
    private var launch: Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            selected = it.getString("server")
            launch = it.getBoolean("launch", true)
            prevEpisode = it.getString("prev")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetSelectorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        var loaded = false
        model.getMedia().observe(viewLifecycleOwner) { m ->
            media = m
            if (media != null && !loaded) {
                loaded = true
                episode = media?.anime?.episodes?.get(media?.anime?.selectedEpisode)
                if (episode != null) {
                    if (selected != null) {
                        binding.selectorListContainer.visibility = View.GONE
                        binding.selectorAutoListContainer.visibility = View.VISIBLE
                        binding.selectorAutoText.text = selected
                        binding.selectorCancel.setOnClickListener {
                            media!!.selected!!.stream = null
                            model.saveSelected(media!!.id, media!!.selected!!, requireActivity())
                            dismiss()
                        }
                        fun fail() {
                            toastString("Couldn't auto select the server, Please try again!")
                            binding.selectorCancel.performClick()
                        }

                        fun load() {
                            if (episode?.streamLinks?.containsKey(selected) == true) {
                                if (episode!!.streamLinks[selected]!!.quality.size >= media!!.selected!!.quality) {
                                    media!!.anime!!.episodes?.get(media!!.anime!!.selectedEpisode!!)?.selectedStream =
                                        selected
                                    media!!.anime!!.episodes?.get(media!!.anime!!.selectedEpisode!!)?.selectedQuality =
                                        media!!.selected!!.quality
                                    startExoplayer(media!!)
                                } else fail()
                            } else fail()
                        }
                        if (episode?.streamLinks?.isEmpty() == true || episode?.saveStreams == false) {
                            model.getEpisode().observe(this) {
                                if (it != null) {
                                    episode = it
                                    load()
                                }
                            }
                            scope.launch {
                                if (withContext(Dispatchers.IO) { !model.loadEpisodeStream(episode!!, media!!.selected!!) }) fail()
                            }
                        } else load()
                    } else {
                        binding.selectorRecyclerView.updateLayoutParams<ViewGroup.MarginLayoutParams> { bottomMargin = navBarHeight }
                        binding.selectorRecyclerView.adapter = null
                        binding.selectorProgressBar.visibility = View.VISIBLE
                        makeDefault = loadData("make_default") ?: true
                        binding.selectorMakeDefault.isChecked = makeDefault
                        binding.selectorMakeDefault.setOnClickListener {
                            makeDefault = binding.selectorMakeDefault.isChecked
                            saveData("make_default", makeDefault)
                        }
                        fun load() {
                            binding.selectorProgressBar.visibility = View.GONE
                            media!!.anime?.episodes?.set(
                                media!!.anime?.selectedEpisode ?: "",
                                episode!!
                            )
                            binding.selectorRecyclerView.layoutManager = LinearLayoutManager(
                                requireActivity(),
                                LinearLayoutManager.VERTICAL,
                                false
                            )
                            binding.selectorRecyclerView.adapter = StreamAdapter()
                        }
                        if (episode!!.streamLinks.isEmpty() || !episode!!.allStreams || episode?.saveStreams == false) {
                            model.getEpisode().observe(this) {
                                if (it != null) {
                                    episode = it
                                    load()
                                }
                            }
                            scope.launch(Dispatchers.IO) {
                                model.loadEpisodeStreams(episode!!, media!!.selected!!.source)
                            }
                        } else load()
                    }
                }
            }
        }

        super.onViewCreated(view, savedInstanceState)
    }

    fun startExoplayer(media: Media) {
        prevEpisode = null

        dismiss()
        if (launch!!) {
            val intent = Intent(activity, ExoplayerView::class.java).apply {
                putExtra("media", media)
            }
            startActivity(intent)
        } else {
            model.setEpisode(media.anime!!.episodes!![media.anime.selectedEpisode!!]!!, "startExo no launch")
        }
    }

    private inner class StreamAdapter : RecyclerView.Adapter<StreamAdapter.StreamViewHolder>() {
        val links = episode!!.streamLinks
        val keys = links.keys.toList()
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StreamViewHolder = StreamViewHolder(ItemStreamBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        override fun onBindViewHolder(holder: StreamViewHolder, position: Int) {
            val server = if (position < keys.size) links[keys[position]]?.server else null
            if (server != null) {
                holder.binding.streamName.text = server
                holder.binding.streamRecyclerView.layoutManager = LinearLayoutManager(requireContext())
                holder.binding.streamRecyclerView.adapter = QualityAdapter(server)
            }
        }
        override fun getItemCount(): Int = links.size
        private inner class StreamViewHolder(val binding: ItemStreamBinding) : RecyclerView.ViewHolder(binding.root)
    }

    private inner class QualityAdapter(private val stream: String) : RecyclerView.Adapter<QualityAdapter.UrlViewHolder>() {
        val urls = episode!!.streamLinks[stream]!!.quality

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UrlViewHolder {
            return UrlViewHolder(ItemUrlBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: UrlViewHolder, position: Int) {
            val binding = holder.binding
            val url = urls[position]
            binding.urlQuality.text = url.quality
            binding.urlNote.text = url.note ?: ""
            binding.urlNote.visibility = if (url.note != null) View.VISIBLE else View.GONE
            if (url.quality != "Multi Quality") {
                binding.urlSize.visibility = if (url.size != null) View.VISIBLE else View.GONE
                binding.urlSize.text = (if (url.note != null) " : " else "") + DecimalFormat("#.##").format(url.size ?: 0).toString() + " MB"
                binding.urlDownload.visibility = View.VISIBLE
                binding.urlDownload.setOnClickListener {
                    media!!.anime!!.episodes!![media!!.anime!!.selectedEpisode!!]!!.selectedStream = stream
                    media!!.anime!!.episodes!![media!!.anime!!.selectedEpisode!!]!!.selectedQuality = position
                    download(requireActivity(), media!!.anime!!.episodes!![media!!.anime!!.selectedEpisode!!]!!, media!!.userPreferredName)
                }
            } else binding.urlDownload.visibility = View.GONE
        }

        override fun getItemCount(): Int = urls.size

        private inner class UrlViewHolder(val binding: ItemUrlBinding) : RecyclerView.ViewHolder(binding.root) {
            init {
                itemView.setOnClickListener {
                    media!!.anime!!.episodes!![media!!.anime!!.selectedEpisode!!]?.selectedStream = stream
                    media!!.anime!!.episodes!![media!!.anime!!.selectedEpisode!!]?.selectedQuality = bindingAdapterPosition
                    if (makeDefault) {
                        media!!.selected!!.stream = stream
                        media!!.selected!!.quality = bindingAdapterPosition
                        model.saveSelected(media!!.id, media!!.selected!!, requireActivity())
                    }
                    startExoplayer(media!!)
                }
            }
        }
    }

    companion object {
        fun newInstance(server: String? = null, la: Boolean = true, prev: String? = null): SelectorDialogFragment =
            SelectorDialogFragment().apply {
                arguments = Bundle().apply {
                    putString("server", server)
                    putBoolean("launch", la)
                    putString("prev", prev)
                }
            }
    }

    override fun onSaveInstanceState(outState: Bundle) {}

    override fun onDismiss(dialog: DialogInterface) {
        if (launch == false) {
            @Suppress("DEPRECATION")
            activity?.hideSystemBars()
            model.epChanged.postValue(true)
            if (prevEpisode != null) {
                media?.anime?.selectedEpisode = prevEpisode
                model.setEpisode(media?.anime?.episodes?.get(prevEpisode) ?: return, "prevEp")
            }
        }
        super.onDismiss(dialog)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
