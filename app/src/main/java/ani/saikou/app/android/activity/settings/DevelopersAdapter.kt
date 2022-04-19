package ani.saikou.app.android.activity.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ani.saikou.app.databinding.ItemDeveloperBinding
import ani.saikou.app.util.loadImage
import ani.saikou.app.util.openLinkInBrowser
import ani.saikou.app.util.setAnimation
import ani.saikou.core.model.settings.Developer
import ani.saikou.core.model.settings.UserInterfaceSettings
import ani.saikou.core.service.STORE

class DevelopersAdapter(private val developers: Array<Developer>) :
    RecyclerView.Adapter<DevelopersAdapter.DeveloperViewHolder>() {
    private val uiSettings =
        STORE.loadData<UserInterfaceSettings>("ui_settings") ?: UserInterfaceSettings()

    inner class DeveloperViewHolder(val binding: ItemDeveloperBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                openLinkInBrowser(developers[bindingAdapterPosition].url)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeveloperViewHolder {
        return DeveloperViewHolder(
            ItemDeveloperBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: DeveloperViewHolder, position: Int) {
        val b = holder.binding
        setAnimation(b.root.context, b.root, uiSettings)
        val dev = developers[position]
        b.devName.text = dev.name
        b.devProfile.loadImage(dev.pfp)
        b.devRole.text = dev.role
    }

    override fun getItemCount(): Int = developers.size
}