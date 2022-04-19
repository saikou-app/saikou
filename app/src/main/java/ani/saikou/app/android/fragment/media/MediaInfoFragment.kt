package ani.saikou.app.android.fragment.media

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import ani.saikou.app.R
import ani.saikou.app.android.activity.media.MediaDetailsActivity
import ani.saikou.app.android.activity.media.StudioActivity
import ani.saikou.app.android.adapter.media.CharacterAdapter
import ani.saikou.app.android.adapter.media.GenreAdapter
import ani.saikou.app.android.adapter.media.MediaAdapter
import ani.saikou.app.android.model.media.MediaDetailsViewModel
import ani.saikou.app.anilist.GenresViewModel
import ani.saikou.app.databinding.*
import ani.saikou.app.util.*
import io.noties.markwon.Markwon
import io.noties.markwon.SoftBreakAddsNewLinePlugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.io.Serializable
import java.net.URLEncoder


@SuppressLint("SetTextI18n")
class MediaInfoFragment : Fragment() {
    private var _binding: FragmentMediaInfoBinding? = null
    private val binding get() = _binding!!
    private var timer: CountDownTimer? = null
    private var loaded = false
    private var type = "ANIME"
    private val genreModel: GenresViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMediaInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView();_binding = null
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val model: MediaDetailsViewModel by activityViewModels()
        binding.mediaInfoProgressBar.visibility = if (!loaded) View.VISIBLE else View.GONE
        binding.mediaInfoContainer.visibility = if (loaded) View.VISIBLE else View.GONE
        binding.mediaInfoContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> { bottomMargin += 128f.px + navBarHeight }

        model.getMedia().observe(viewLifecycleOwner) { media ->
            if (media != null && !loaded) {
                loaded = true
                binding.mediaInfoProgressBar.visibility = View.GONE
                binding.mediaInfoContainer.visibility = View.VISIBLE
                binding.mediaInfoName.text = "\t\t\t" + media.mainName
                binding.mediaInfoName.setOnLongClickListener {
                    copyToClipboard(media.mainName)
                    true
                }
                if (media.name != "null") binding.mediaInfoNameRomajiContainer.visibility =
                    View.VISIBLE
                binding.mediaInfoNameRomaji.text = "\t\t\t" + media.nameRomaji
                binding.mediaInfoNameRomaji.setOnLongClickListener {
                    copyToClipboard(media.nameRomaji)
                    true
                }
                binding.mediaInfoMeanScore.text =
                    if (media.meanScore != null) (media.meanScore!! / 10.0).toString() else "??"
                binding.mediaInfoStatus.text = media.status
                binding.mediaInfoFormat.text = media.format?.replace("_", " ")
                binding.mediaInfoSource.text = media.source
                binding.mediaInfoStart.text =
                    if (media.startDate.toString() != "") media.startDate.toString() else "??"
                binding.mediaInfoEnd.text =
                    if (media.endDate.toString() != "") media.endDate.toString() else "??"
                if (media.anime != null) {
                    val anime = media.anime!!

                    binding.mediaInfoDuration.text =
                        if (anime.episodeDuration != null) anime.episodeDuration.toString() else "??"
                    binding.mediaInfoDurationContainer.visibility = View.VISIBLE
                    binding.mediaInfoSeasonContainer.visibility = View.VISIBLE
                    binding.mediaInfoSeason.text =
                        anime.season ?: "??" + " " + anime.seasonYear
                    if (anime.mainStudio != null) {
                        binding.mediaInfoStudioContainer.visibility = View.VISIBLE
                        binding.mediaInfoStudio.text = anime.mainStudio!!.name
                        binding.mediaInfoStudioContainer.setOnClickListener {
                            ContextCompat.startActivity(
                                requireActivity(),
                                Intent(activity, StudioActivity::class.java).putExtra(
                                    "studio",
                                    anime.mainStudio!! as Serializable
                                ),
                                null
                            )
                        }
                    }
                    binding.mediaInfoTotalTitle.setText(R.string.total_eps)
                    binding.mediaInfoTotal.text =
                        if (anime.nextAiringEpisode != null) (anime.nextAiringEpisode.toString() + " | " + (anime.totalEpisodes
                            ?: "~").toString()) else (anime.totalEpisodes ?: "~").toString()
                } else if (media.manga != null) {
                    type = "MANGA"
                    binding.mediaInfoTotalTitle.setText(R.string.total_chaps)
                    binding.mediaInfoTotal.text = (media.manga!!.totalChapters ?: "~").toString()
                }

                val desc = HtmlCompat.fromHtml(
                    (media.description ?: "null").replace("\\n", "<br>").replace("\\\"", "\""),
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                )
                binding.mediaInfoDescription.text =
                    "\t\t\t" + if (desc.toString() != "null") desc else "No Description Available"
                binding.mediaInfoDescription.setOnClickListener {
                    if (binding.mediaInfoDescription.maxLines == 5) {
                        ObjectAnimator.ofInt(binding.mediaInfoDescription, "maxLines", 100)
                            .setDuration(950).start()
                    } else {
                        ObjectAnimator.ofInt(binding.mediaInfoDescription, "maxLines", 5)
                            .setDuration(400).start()
                    }
                }

                countDown(media, binding.mediaInfoContainer)
                val parent = _binding?.mediaInfoContainer!!
                val screenWidth = resources.displayMetrics.run { widthPixels / density }

                if (media.synonyms.isNotEmpty()) {
                    val bind = ItemTitleChipgroupBinding.inflate(
                        LayoutInflater.from(context),
                        parent,
                        false
                    )
                    for (position in media.synonyms.indices) {
                        val chip = ItemChipBinding.inflate(
                            LayoutInflater.from(context),
                            bind.itemChipGroup,
                            false
                        ).root
                        chip.text = media.synonyms[position]
                        chip.setOnLongClickListener { copyToClipboard(media.synonyms[position]);true }
                        bind.itemChipGroup.addView(chip)
                    }
                    parent.addView(bind.root)
                }

                if (media.trailer != null) {
                    @Suppress("DEPRECATION")
                    class MyChrome : WebChromeClient() {
                        private var mCustomView: View? = null
                        private var mCustomViewCallback: CustomViewCallback? = null
                        private var mOriginalSystemUiVisibility = 0

                        override fun onHideCustomView() {
                            (requireActivity().window.decorView as FrameLayout).removeView(
                                mCustomView
                            )
                            mCustomView = null
                            requireActivity().window.decorView.systemUiVisibility =
                                mOriginalSystemUiVisibility
                            mCustomViewCallback!!.onCustomViewHidden()
                            mCustomViewCallback = null
                        }

                        override fun onShowCustomView(
                            paramView: View,
                            paramCustomViewCallback: CustomViewCallback
                        ) {
                            if (mCustomView != null) {
                                onHideCustomView()
                                return
                            }
                            mCustomView = paramView
                            mOriginalSystemUiVisibility =
                                requireActivity().window.decorView.systemUiVisibility
                            mCustomViewCallback = paramCustomViewCallback
                            (requireActivity().window.decorView as FrameLayout).addView(
                                mCustomView,
                                FrameLayout.LayoutParams(-1, -1)
                            )
                            requireActivity().window.decorView.systemUiVisibility =
                                3846 or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        }
                    }

                    val bind = ItemTitleTrailerBinding.inflate(
                        LayoutInflater.from(context),
                        parent,
                        false
                    )
                    bind.mediaInfoTrailer.apply {
                        visibility = View.VISIBLE
                        settings.javaScriptEnabled = true
                        isSoundEffectsEnabled = true
                        webChromeClient = MyChrome()
                        loadUrl(media.trailer!!)
                    }
                    parent.addView(bind.root)
                }

                if (media.anime != null) {
                    val anime = media.anime!!
                    if (anime.op.isNotEmpty() || anime.ed.isNotEmpty()) {
                        val markWon = Markwon.builder(requireContext())
                            .usePlugin(SoftBreakAddsNewLinePlugin.create()).build()

                        @Suppress("BlockingMethodInNonBlockingContext")
                        fun makeLink(a: String): String {
                            val first = a.indexOf('"').let { if (it != -1) it else return a } + 1
                            val end = a.indexOf('"', first).let { if (it != -1) it else return a }
                            val name = a.subSequence(first, end).toString()
                            return "${
                                a.subSequence(
                                    0,
                                    first
                                )
                            }[$name](https://www.youtube.com/results?search_query=${
                                URLEncoder.encode(
                                    name,
                                    "utf-8"
                                )
                            })${a.subSequence(end, a.length)}"
                        }

                        fun makeText(textView: TextView, arr: List<String>) {
                            var op = ""
                            arr.forEach {
                                op += "\n"
                                op += makeLink(it)
                            }
                            op = op.removePrefix("\n")
                            textView.setOnClickListener {
                                if (textView.maxLines == 4) {
                                    ObjectAnimator.ofInt(textView, "maxLines", 100)
                                        .setDuration(950).start()
                                } else {
                                    ObjectAnimator.ofInt(textView, "maxLines", 4)
                                        .setDuration(400).start()
                                }
                            }
                            markWon.setMarkdown(textView, op)
                        }

                        if (anime.op.isNotEmpty()) {
                            val bind = ItemTitleTextBinding.inflate(
                                LayoutInflater.from(context),
                                parent,
                                false
                            )
                            bind.itemTitle.setText(R.string.opening)
                            makeText(bind.itemText, anime.op)
                            parent.addView(bind.root)
                        }


                        if (anime.ed.isNotEmpty()) {
                            val bind = ItemTitleTextBinding.inflate(
                                LayoutInflater.from(context),
                                parent,
                                false
                            )
                            bind.itemTitle.setText(R.string.ending)
                            makeText(bind.itemText, anime.ed)
                            parent.addView(bind.root)
                        }
                    }
                }

                if (media.genres.isNotEmpty()) {
                    val bind = ActivityGenreBinding.inflate(
                        LayoutInflater.from(context),
                        parent,
                        false
                    )
                    val adapter = GenreAdapter(type)
                    genreModel.doneListener = {
                        MainScope().launch {
                            bind.mediaInfoGenresProgressBar.visibility = View.GONE
                        }
                    }
                    if (genreModel.genres != null) {
                        adapter.genres = genreModel.genres!!
                        adapter.pos = ArrayList(genreModel.genres!!.keys)
                        if (genreModel.done) genreModel.doneListener?.invoke()
                    }
                    bind.mediaInfoGenresRecyclerView.adapter = adapter
                    bind.mediaInfoGenresRecyclerView.layoutManager =
                        GridLayoutManager(requireActivity(), (screenWidth / 156f).toInt())

                    lifecycleScope.launch(Dispatchers.IO) {
                        genreModel.loadGenres(media.genres) {
                            MainScope().launch {
                                adapter.addGenre(it)
                            }
                        }
                    }
                    parent.addView(bind.root)
                }

                if (media.tags.isNotEmpty()) {
                    val bind = ItemTitleChipgroupBinding.inflate(
                        LayoutInflater.from(context),
                        parent,
                        false
                    )
                    bind.itemTitle.setText(R.string.tags)
                    for (position in media.tags.indices) {
                        val chip = ItemChipBinding.inflate(
                            LayoutInflater.from(context),
                            bind.itemChipGroup,
                            false
                        ).root
                        chip.text = media.tags[position]
                        chip.setOnLongClickListener { copyToClipboard(media.tags[position]);true }
                        bind.itemChipGroup.addView(chip)
                    }
                    parent.addView(bind.root)
                }

                if (!media.characters.isNullOrEmpty()) {
                    val bind = ItemTitleRecyclerBinding.inflate(
                        LayoutInflater.from(context),
                        parent,
                        false
                    )
                    bind.itemTitle.setText(R.string.characters)
                    bind.itemRecycler.adapter =
                        CharacterAdapter(media.characters!!)
                    bind.itemRecycler.layoutManager = LinearLayoutManager(
                        requireContext(),
                        LinearLayoutManager.HORIZONTAL,
                        false
                    )
                    parent.addView(bind.root)
                }

                if (!media.relations.isNullOrEmpty()) {
                    if (media.sequel != null || media.prequel != null) {
                        val bind = ItemQuelsBinding.inflate(
                            LayoutInflater.from(context),
                            parent,
                            false
                        )

                        if (media.sequel != null) {
                            bind.mediaInfoSequel.visibility = View.VISIBLE
                            bind.mediaInfoSequelImage.loadImage(
                                media.sequel!!.banner ?: media.sequel!!.cover
                            )
                            bind.mediaInfoSequel.setSafeOnClickListener {
                                ContextCompat.startActivity(
                                    requireContext(),
                                    Intent(
                                        requireContext(),
                                        MediaDetailsActivity::class.java
                                    ).putExtra(
                                        "media",
                                        media.sequel as Serializable
                                    ), null
                                )
                            }
                        }
                        if (media.prequel != null) {
                            bind.mediaInfoPrequel.visibility = View.VISIBLE
                            bind.mediaInfoPrequelImage.loadImage(
                                media.prequel!!.banner ?: media.prequel!!.cover
                            )
                            bind.mediaInfoPrequel.setSafeOnClickListener {
                                ContextCompat.startActivity(
                                    requireContext(),
                                    Intent(
                                        requireContext(),
                                        MediaDetailsActivity::class.java
                                    ).putExtra(
                                        "media",
                                        media.prequel as Serializable
                                    ), null
                                )
                            }
                        }
                        parent.addView(bind.root)
                    }

                    val bindi = ItemTitleRecyclerBinding.inflate(
                        LayoutInflater.from(context),
                        parent,
                        false
                    )

                    bindi.itemRecycler.adapter =
                        MediaAdapter(0, media.relations!!, requireActivity())
                    bindi.itemRecycler.layoutManager = LinearLayoutManager(
                        requireContext(),
                        LinearLayoutManager.HORIZONTAL,
                        false
                    )
                    parent.addView(bindi.root)
                }

                if (!media.recommendations.isNullOrEmpty()) {
                    val bind = ItemTitleRecyclerBinding.inflate(
                        LayoutInflater.from(context),
                        parent,
                        false
                    )
                    bind.itemTitle.setText(R.string.recommended)
                    bind.itemRecycler.adapter =
                        MediaAdapter(0, media.recommendations!!, requireActivity())
                    bind.itemRecycler.layoutManager = LinearLayoutManager(
                        requireContext(),
                        LinearLayoutManager.HORIZONTAL,
                        false
                    )
                    parent.addView(bind.root)
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val cornerTop = ObjectAnimator.ofFloat(binding.root, "radius", 0f, 32f).setDuration(200)
            val cornerNotTop =
                ObjectAnimator.ofFloat(binding.root, "radius", 32f, 0f).setDuration(200)
            var cornered = true
            cornerTop.start()
            binding.mediaInfoScroll.setOnScrollChangeListener { v, _, _, _, _ ->
                if (!v.canScrollVertically(-1)) {
                    if (!cornered) {
                        cornered = true
                        cornerTop.start()
                    }
                } else {
                    if (cornered) {
                        cornered = false
                        cornerNotTop.start()
                    }
                }
            }
        }
        super.onViewCreated(view, null)
    }

    override fun onResume() {
        binding.mediaInfoProgressBar.visibility = if (!loaded) View.VISIBLE else View.GONE
        super.onResume()
    }

    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
    }
}
