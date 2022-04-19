package ani.saikou.app.android.activity

import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import ani.saikou.app.databinding.ActivityNoInternetBinding
import ani.saikou.app.util.isOnline
import ani.saikou.app.util.navBarHeight
import ani.saikou.app.util.startMainActivity
import ani.saikou.app.util.statusBarHeight

class NoInternetActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityNoInternetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.refreshContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = statusBarHeight
            bottomMargin = navBarHeight
        }
        binding.refreshButton.setOnClickListener {
            if (isOnline(this)) {
                startMainActivity(this)
            }
        }
    }
}