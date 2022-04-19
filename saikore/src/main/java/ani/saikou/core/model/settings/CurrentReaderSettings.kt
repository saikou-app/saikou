package ani.saikou.core.model.settings

import java.io.Serializable

data class CurrentReaderSettings(
    var direction: Directions = Directions.TOP_TO_BOTTOM,
    var layout: Layouts = Layouts.CONTINUOUS,
    var padding: Boolean = true,
) : Serializable {
    enum class Directions {
        TOP_TO_BOTTOM, RIGHT_TO_LEFT, BOTTOM_TO_TOP, LEFT_TO_RIGHT
    }

    enum class Layouts {
        PAGED, CONTINUOUS_PAGED, CONTINUOUS
    }
}