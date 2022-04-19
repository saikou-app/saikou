package ani.saikou.app.android.model.user

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ani.saikou.app.util.anilist.anilist.Anilist
import ani.saikou.core.model.media.Media
import ani.saikou.core.service.STORE

class ListViewModel : ViewModel() {
    var grid = MutableLiveData(STORE.loadData<Boolean>("listGrid") ?: true)

    private val lists = MutableLiveData<MutableMap<String, ArrayList<Media>>>()
    fun getLists(): LiveData<MutableMap<String, ArrayList<Media>>> = lists
    fun loadLists(anime: Boolean, userId: Int) {
        lists.postValue(Anilist.query.getMediaLists(anime, userId))
    }
}