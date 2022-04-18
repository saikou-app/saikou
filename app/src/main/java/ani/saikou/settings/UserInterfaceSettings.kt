package ani.saikou.settings

import java.io.Serializable

data class UserInterfaceSettings(
    var darkMode: Boolean? = null,
    var showYtButton: Boolean = true,
    var animeDefaultView: Int = 0,
    var mangaDefaultView: Int = 0,

    //App
    var immersiveMode: Boolean = false,
    var smallView: Boolean = true,
    var defaultStartUpTab: Int = 1,
    var homeLayoutShow: ArrayList<Boolean> = arrayListOf(true, false, true, false, true),

    //Animations
    var bannerAnimations: Boolean = true,
    var layoutAnimations: Boolean = true,
    var animationSpeed: Float = 1f

) : Serializable