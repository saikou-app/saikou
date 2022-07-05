package ani.saikou.anime

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import ani.saikou.parsers.VideoExtractor
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
                val ep =  media?.anime?.episodes?.get(media?.anime?.selectedEpisode)
                episode = ep
                if (ep != null) {

                    if (selected != null) {
                        binding.selectorListContainer.visibility = View.GONE
                        binding.selectorAutoListContainer.visibility = View.VISIBLE
                        binding.selectorAutoText.text = selected
                        binding.selectorCancel.setOnClickListener {
                            media!!.selected!!.server = null
                            model.saveSelected(media!!.id, media!!.selected!!, requireActivity())
                            dismiss()
                        }
                        fun fail() {
                            toastString("Couldn't auto select the server, Please try again!")
                            binding.selectorCancel.performClick()
                        }

                        fun load() {
                            val size = ep.extractors?.find { it.server.name == selected }?.videos?.size
                            if (size!=null && size >= media!!.selected!!.video) {
                                media!!.anime!!.episodes?.get(media!!.anime!!.selectedEpisode!!)?.selectedServer = selected
                                media!!.anime!!.episodes?.get(media!!.anime!!.selectedEpisode!!)?.selectedVideo = media!!.selected!!.video
                                startExoplayer(media!!)
                            } else fail()
                        }
                        
                        if (ep.extractors.isNullOrEmpty()) {
                            model.getEpisode().observe(this) {
                                if (it != null) {
                                    episode = it
                                    load()
                                }
                            }
                            scope.launch {
                                if (withContext(Dispatchers.IO) {
                                        !model.loadEpisodeSingleVideo(
                                            ep,
                                            media!!.selected!!
                                        )
                                    }) fail()
                            }
                        } else load()
                    }
                    else {
                        binding.selectorRecyclerView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                            bottomMargin = navBarHeight
                        }
                        binding.selectorRecyclerView.adapter = null
                        binding.selectorProgressBar.visibility = View.VISIBLE
                        makeDefault = loadData("make_default") ?: true
                        binding.selectorMakeDefault.isChecked = makeDefault
                        binding.selectorMakeDefault.setOnClickListener {
                            makeDefault = binding.selectorMakeDefault.isChecked
                            saveData("make_default", makeDefault)
                        }
                        binding.selectorRecyclerView.layoutManager =
                            LinearLayoutManager(requireActivity(), LinearLayoutManager.VERTICAL, false)
                        val adapter = ExtractorAdapter()
                        binding.selectorRecyclerView.adapter = adapter
                        if (!ep.allStreams ) {
                            ep.extractorCallback = {
                                scope.launch {
                                    adapter.add(it)
                                }
                            }
                            model.getEpisode().observe(this) {
                                if (it != null) {
                                    media!!.anime?.episodes?.set(media!!.anime?.selectedEpisode!!, ep)
                                }
                            }
                            scope.launch(Dispatchers.IO) {
                                model.loadEpisodeVideos(ep, media!!.selected!!.source)
                                withContext(Dispatchers.Main){
                                    binding.selectorProgressBar.visibility = View.GONE
                                }
                            }
                        } else {
                            media!!.anime?.episodes?.set(media!!.anime?.selectedEpisode!!, ep)
                            adapter.addAll(ep.extractors)
                            binding.selectorProgressBar.visibility = View.GONE
                        }
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
            stopAddingToList()
            val intent = Intent(activity, ExoplayerView::class.java).apply {
                putExtra("media", media)
            }
            startActivity(intent)
        } else {
            model.setEpisode(media.anime!!.episodes!![media.anime.selectedEpisode!!]!!, "startExo no launch")
        }
    }

    private fun stopAddingToList() {
        episode?.extractorCallback = null
        episode?.also {
            it.extractors = it.extractors?.toMutableList()
        }
    }

    private inner class ExtractorAdapter : RecyclerView.Adapter<ExtractorAdapter.StreamViewHolder>() {
        val links = mutableListOf<VideoExtractor>()
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StreamViewHolder =
            StreamViewHolder(ItemStreamBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: StreamViewHolder, position: Int) {
            val extractor = links[position]
            holder.binding.streamName.text = extractor.server.name

                holder.binding.streamRecyclerView.layoutManager = LinearLayoutManager(requireContext())
                holder.binding.streamRecyclerView.adapter = VideoAdapter(extractor)

        }

        override fun getItemCount(): Int = links.size

        fun add(videoExtractor: VideoExtractor){
            if(videoExtractor.videos.isNotEmpty()) {
                links.add(videoExtractor)
                notifyItemInserted(links.size - 1)
            }
        }

        fun addAll(extractors: List<VideoExtractor>?) {
            links.addAll(extractors?:return)
            notifyItemRangeInserted(0,extractors.size)
        }

        private inner class StreamViewHolder(val binding: ItemStreamBinding) : RecyclerView.ViewHolder(binding.root)
    }

    private inner class VideoAdapter(private val extractor : VideoExtractor) : RecyclerView.Adapter<VideoAdapter.UrlViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UrlViewHolder {
            return UrlViewHolder(ItemUrlBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: UrlViewHolder, position: Int) {
            val binding = holder.binding
            val video = extractor.videos[position]
            binding.urlQuality.text = if(video.quality!=null) "${video.quality}p" else "Default Quality"
            binding.urlNote.text = video.extraNote ?: ""
            binding.urlNote.visibility = if (video.extraNote != null) View.VISIBLE else View.GONE
            if (!video.isM3U8) {
                binding.urlSize.visibility = if (video.size != null) View.VISIBLE else View.GONE
                binding.urlSize.text =
                    (if (video.extraNote != null) " : " else "") + DecimalFormat("#.##").format(video.size ?: 0).toString() + " MB"
                binding.urlDownload.visibility = View.VISIBLE
                binding.urlDownload.setSafeOnClickListener {
                    media!!.anime!!.episodes!![media!!.anime!!.selectedEpisode!!]!!.selectedServer = extractor.server.name
                    media!!.anime!!.episodes!![media!!.anime!!.selectedEpisode!!]!!.selectedVideo = position
                    download(
                        requireActivity(),
                        media!!.anime!!.episodes!![media!!.anime!!.selectedEpisode!!]!!,
                        media!!.userPreferredName
                    )
                }
            }
            else {
                binding.urlQuality.text = "Multi Quality"
                binding.urlDownload.visibility = View.GONE
            }
        }

        override fun getItemCount(): Int = extractor.videos.size

        private fun isPackageInstalled(packageName: String, packageManager: PackageManager): Boolean {
            return try {
                packageManager.getPackageInfo(packageName, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }

        private inner class UrlViewHolder(val binding: ItemUrlBinding) : RecyclerView.ViewHolder(binding.root) {
            init {
                itemView.setSafeOnClickListener {
                    media!!.anime!!.episodes!![media!!.anime!!.selectedEpisode!!]?.selectedServer = extractor.server.name
                    media!!.anime!!.episodes!![media!!.anime!!.selectedEpisode!!]?.selectedVideo = bindingAdapterPosition
                    if (makeDefault) {
                        media!!.selected!!.server = extractor.server.name
                        media!!.selected!!.video = bindingAdapterPosition
                        model.saveSelected(media!!.id, media!!.selected!!, requireActivity())
                    }
                    startExoplayer(media!!)
                }
                itemView.setOnLongClickListener {
                    val video = extractor.videos[position]
                    copyToClipboard(video.url.url, false)
                    // 1DM integration
                    val episode = media!!.anime!!.episodes!![media!!.anime!!.selectedEpisode!!]!!
                    val pm = context!!.packageManager
                    val appName = if (isPackageInstalled("idm.internet.download.manager.plus", pm)) {
                        "idm.internet.download.manager.plus"
                    } else if (isPackageInstalled("idm.internet.download.manager", pm)) {
                        "idm.internet.download.manager"
                    } else {
                        ""
                    }
                    if (appName != "") {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            addCategory(Intent.CATEGORY_DEFAULT)
                            data = Uri.parse(video.url.url)
                            putExtra("extra_filename", "[EP " + episode.number + "] " + episode.title.toString())
                            setPackage(appName)
                            setClassName("idm.internet.download.manager.plus", "idm.internet.download.manager.Downloader")
                        }
                        startActivity(intent)
                        toast("Link sent to 1DM"); true
                    }
                    else{
                        if (video.isM3U8) toast("Copied m3u8 URL to clipboard")
                        else toast("Copied video URL to clipboard"); true
                    }
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
