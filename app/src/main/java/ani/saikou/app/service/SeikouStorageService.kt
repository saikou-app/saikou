package ani.saikou.app.service

import ani.saikou.core.service.IStorageService

/**
 * [IStorageService] implementation based on Android APIs.
 *
 * @author xtrm
 */
object SeikouStorageService: IStorageService {
    init {
        IStorageService.INSTANCE = this
    }

    override fun saveData(fileName: String, data: Any?) =
        ani.saikou.app.util.saveData(fileName, data)

    override fun <T> loadData(fileName: String, notify: Boolean): T? =
        ani.saikou.app.util.loadData<T>(fileName, null, notify)
}