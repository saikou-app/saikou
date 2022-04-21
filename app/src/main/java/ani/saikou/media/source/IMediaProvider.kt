package ani.saikou.media.source

import ani.saikou.Named
import ani.saikou.media.Media

/**
 * Abstract media provider.
 *
 * @author xtrm
 * @since 1.2.0
 */
interface IMediaProvider: Named {
    /**
     * Does this provider contain adult-rated content.
     */
    val isAdult: Boolean

    /**
     * The media type that this provider supports.
     */
    val mediaType: Media.Type
}