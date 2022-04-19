package ani.saikou.core.source

import java.net.URL

/**
 * Super-interface for all parsers.
 *
 * @author xtrm
 */
interface IParser {
    /**
     * The parsed service's name.
     */
    val name: String

    /**
     * Whether the service provides adult content.
     */
    val isAdult: Boolean

    /**
     * Checks if the provided URL is supported by the parser.
     *
     * @param url The URL to check.
     *
     * @return whether the URL is supported.
     */
    fun canParse(url: URL): Boolean
}