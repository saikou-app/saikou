package ani.saikou.tv

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import ani.saikou.R

class TVAnimeDetailActivity: FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tv_detail_activity)

        supportFragmentManager.beginTransaction()
            .replace(R.id.main_detail_fragment, TVAnimeDetailFragment(), "Detail")
            .commitNow()
    }
}