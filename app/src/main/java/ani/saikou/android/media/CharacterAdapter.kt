package ani.saikou.android.media

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.util.Pair
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import ani.saikou.android.databinding.ItemCharacterBinding
import ani.saikou.android.loadData
import ani.saikou.android.loadImage
import ani.saikou.android.setAnimation
import ani.saikou.core.model.media.Character
import ani.saikou.core.model.settings.UserInterfaceSettings
import java.io.Serializable

class CharacterAdapter(
    private val characterList: List<Character>
) : RecyclerView.Adapter<CharacterAdapter.CharacterViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CharacterViewHolder {
        val binding =
            ItemCharacterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CharacterViewHolder(binding)
    }

    private val uiSettings =
        loadData<UserInterfaceSettings>("ui_settings") ?: UserInterfaceSettings()

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: CharacterViewHolder, position: Int) {
        val binding = holder.binding
        setAnimation(binding.root.context, holder.binding.root, uiSettings)
        val character = characterList[position]
        binding.itemCompactRelation.text = character.role + "  "
        binding.itemCompactImage.loadImage(character.image)
        binding.itemCompactTitle.text = character.name
    }

    override fun getItemCount(): Int = characterList.size
    inner class CharacterViewHolder(val binding: ItemCharacterBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                val char = characterList[bindingAdapterPosition]
                ContextCompat.startActivity(
                    itemView.context,
                    Intent(
                        itemView.context,
                        CharacterDetailsActivity::class.java
                    ).putExtra("character", char as Serializable),
                    ActivityOptionsCompat.makeSceneTransitionAnimation(
                        itemView.context as Activity,
                        Pair.create(
                            binding.itemCompactImage,
                            ViewCompat.getTransitionName(binding.itemCompactImage)!!
                        ),
                    ).toBundle()
                )
            }
        }
    }
}