package ani.saikou.app.android.fragment.media

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.InputFilter.LengthFilter
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import ani.saikou.app.R
import ani.saikou.app.anilist.Anilist
import ani.saikou.app.databinding.BottomSheetMediaListSmallBinding
import ani.saikou.app.util.*
import ani.saikou.core.model.media.Media
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Serializable


class MediaListDialogSmallFragment : BottomSheetDialogFragment() {

    private lateinit var media: Media

    companion object {
        fun newInstance(m: Media): MediaListDialogSmallFragment =
            MediaListDialogSmallFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("media", m as Serializable)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            media = it.getSerializable("media") as Media
        }
    }

    private var _binding: BottomSheetMediaListSmallBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetMediaListSmallBinding.inflate(inflater, container, false)
        return binding.root
    }


    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.mediaListContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> { bottomMargin += navBarHeight }
        val scope = viewLifecycleOwner.lifecycleScope

        binding.mediaListProgressBar.visibility = View.GONE
        binding.mediaListLayout.visibility = View.VISIBLE

        val statuses: Array<String> = resources.getStringArray(R.array.status)
        binding.mediaListStatus.setText(if (media.userStatus != null) media.userStatus else statuses[0])
        binding.mediaListStatus.setAdapter(
            ArrayAdapter(
                requireContext(),
                R.layout.item_dropdown,
                statuses
            )
        )


        var total: Int? = null
        binding.mediaListProgress.setText(if (media.userProgress != null) media.userProgress.toString() else "")
        if (media.anime != null) if (media.anime!!.totalEpisodes != null) {
            total = media.anime!!.totalEpisodes!!;binding.mediaListProgress.filters =
                arrayOf(
                    InputFilterMinMax(0.0, total.toDouble(), binding.mediaListStatus),
                    LengthFilter(total.toString().length)
                )
        } else if (media.manga != null) if (media.manga!!.totalChapters != null) {
            total = media.manga!!.totalChapters!!;binding.mediaListProgress.filters =
                arrayOf(
                    InputFilterMinMax(0.0, total.toDouble(), binding.mediaListStatus),
                    LengthFilter(total.toString().length)
                )
        }
        binding.mediaListProgressLayout.suffixText = " / ${total ?: '?'}"
        binding.mediaListProgressLayout.suffixTextView.updateLayoutParams {
            height = ViewGroup.LayoutParams.MATCH_PARENT
        }
        binding.mediaListProgressLayout.suffixTextView.gravity = Gravity.CENTER

        binding.mediaListScore.setText(
            if (media.userScore != 0) media.userScore.div(
                10.0
            ).toString() else ""
        )
        binding.mediaListScore.filters =
            arrayOf(InputFilterMinMax(1.0, 10.0), LengthFilter(10.0.toString().length))
        binding.mediaListScoreLayout.suffixTextView.updateLayoutParams {
            height = ViewGroup.LayoutParams.MATCH_PARENT
        }
        binding.mediaListScoreLayout.suffixTextView.gravity = Gravity.CENTER

        binding.mediaListIncrement.setOnClickListener {
            if (binding.mediaListStatus.text.toString() == statuses[0]) binding.mediaListStatus.setText(
                statuses[1],
                false
            )
            val init =
                if (binding.mediaListProgress.text.toString() != "") binding.mediaListProgress.text.toString()
                    .toInt() else 0
            if (init < total ?: 5000) binding.mediaListProgress.setText((init + 1).toString())
            if (init + 1 == total ?: 5000) {
                binding.mediaListStatus.setText(statuses[2], false)
            }
        }

        binding.mediaListSave.setOnClickListener {
            scope.launch {
                withContext(Dispatchers.IO) {
                    Anilist.mutation.editList(
                        media.id,
                        if (_binding?.mediaListProgress?.text.toString() != "") _binding?.mediaListProgress?.text.toString()
                            .toInt() else null,
                        if (_binding?.mediaListScore?.text.toString() != "") (_binding?.mediaListScore?.text.toString()
                            .toDouble() * 10).toInt() else null,
                        if (_binding?.mediaListStatus?.text.toString() != "") _binding?.mediaListStatus?.text.toString() else null
                    )
                }
                Refresh.all()
                toastString("List Updated")
                dismissAllowingStateLoss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
