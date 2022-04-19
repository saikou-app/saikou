package ani.saikou.core.service

val STORE = IStorageService.INSTANCE

/**
 * @author xtrm
 */
interface IStorageService {
    companion object {
        lateinit var INSTANCE: IStorageService
    }

    fun saveData(fileName: String, data: Any?)

    fun <T> loadData(fileName: String, notify: Boolean = true): T?
}