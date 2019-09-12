package net.corda.node.utilities

import net.corda.core.utilities.seconds
import org.slf4j.LoggerFactory
import kotlin.concurrent.thread

/**
 * Log error message and terminate the process. This might not clean up resources and could leave
 * the system in a messy state.
 */
@Synchronized
fun errorAndTerminate(message: String, e: Throwable? = null) {
    thread {
        val log = LoggerFactory.getLogger("errorAndTerminate")
        if (e != null) log.error(message, e) else log.error(message)
    }

    // give the logger a chance to flush the error message before killing the node
    Thread.sleep(10.seconds.toMillis())
    Runtime.getRuntime().halt(1)
}