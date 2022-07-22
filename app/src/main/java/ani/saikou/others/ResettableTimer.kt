package ani.saikou.others

import java.util.*
import java.util.concurrent.atomic.*

class ResettableTimer {
    var resetLock = AtomicBoolean(false)
    var timer = Timer()
    fun reset(timerTask: TimerTask, delay: Long) {
        if (!resetLock.getAndSet(true)) {
            timer.cancel()
            timer.purge()
            timer = Timer()
            timer.schedule(object : TimerTask() {
                override fun run() {
                    if (!resetLock.getAndSet(true)) {
                        timerTask.run()
                        timer.cancel()
                        timer.purge()
                        resetLock.set(false)
                    }
                }
            }, delay)
            resetLock.set(false)
        }
    }
}