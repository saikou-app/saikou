package ani.saikou.app.service

import ani.saikou.core.service.ILoggingService

/**
 * [ILoggingService] implementation based on Android APIs.
 *
 * @author xtrm
 */
object SeikouLoggingService: ILoggingService {
    init {
        ILoggingService.INSTANCE = this
    }

    override fun log(message: Any?, log: Boolean) =
        ani.saikou.app.util.logger(message?.toString(), log)

    override fun notify(message: Any?) =
        message?.let {
            ani.saikou.app.util.toastString(it.toString())
        } ?: Unit

}