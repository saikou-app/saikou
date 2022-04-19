package ani.saikou.core.service

val LOG = ILoggingService.INSTANCE

/**
 * @author xtrm
 */
interface ILoggingService {
    companion object {
        lateinit var INSTANCE: ILoggingService
    }

    /**
     * Logs a message.
     *
     * @param message the message to log
     * @param log whether to log the message
     */
    fun log(message: Any?, log: Boolean = true)

    /**
     * Displays a notification message.
     *
     * @param message the message to display
     */
    fun notify(message: Any?)
}