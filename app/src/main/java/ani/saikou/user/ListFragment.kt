package ani.saikou.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import ani.saikou.databinding.FragmentListBinding
import ani.saikou.media.Media
import ani.saikou.media.MediaAdaptor

class ListFragment : Fragment() {
    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!
    private var pos: Int? = null
    private var grid: Boolean? = null
    private var list: ArrayList<Media>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            pos = it.getInt("list")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val model: ListViewModel by activityViewModels()
        val screenWidth = resources.displayMetrics.run { widthPixels / density }

        fun update() {
            if (grid != null && list != null) {
                val adapter = MediaAdaptor(if (grid!!) 0 else 1, list!!, requireActivity(), true)
                binding.listRecyclerView.layoutManager = GridLayoutManager(requireContext(), if (grid!!) (screenWidth / 124f).toInt() else 1)
                binding.listRecyclerView.adapter = adapter
            }
        }

        model.getLists().observe(viewLifecycleOwner) {
            if (it != null) {
                list = it.values.toList().getOrNull(pos!!)
                update()
            }
        }
        model.grid.observe(viewLifecycleOwner) {
            grid = it
            update()
        }
    }

    companion object {
        fun newInstance(pos: Int): ListFragment =
            ListFragment().apply {
                arguments = Bundle().apply {
                    putInt("list", pos)
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
