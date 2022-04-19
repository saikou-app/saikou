package ani.saikou.app.android.model.media

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ani.saikou.app.util.anilist.anilist.Anilist
import ani.saikou.core.model.media.Character
import ani.saikou.core.model.media.Studio

class OtherDetailsViewModel : ViewModel() {
    private val character: MutableLiveData<Character> = MutableLiveData(null)
    fun getCharacter(): LiveData<Character> = character
    fun loadCharacter(m: Character) {
        if (character.value == null) character.postValue(Anilist.query.getCharacterDetails(m))
    }

    private val studio: MutableLiveData<Studio> = MutableLiveData(null)
    fun getStudio(): LiveData<Studio> = studio
    fun loadStudio(m: Studio) {
        if (studio.value == null) studio.postValue(Anilist.query.getStudioDetails(m))
    }
}