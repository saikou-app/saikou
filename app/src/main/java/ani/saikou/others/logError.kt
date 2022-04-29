package ani.saikou.others

import ani.saikou.toastString

fun logError(e:Exception) {
    toastString(e.toString())
}