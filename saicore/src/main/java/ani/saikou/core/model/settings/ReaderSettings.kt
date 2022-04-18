package ani.saikou.core.model.settings

import java.io.Serializable

data class ReaderSettings(
    var askIndividual: Boolean = true,
    var updateForH: Boolean = false,
    var readPercentage: Int = 1,
    var default: CurrentReaderSettings = CurrentReaderSettings()
) : Serializable