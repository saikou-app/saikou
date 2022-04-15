package ani.saikou.media

import android.graphics.Color
import android.text.Spannable
import android.text.Spanned
import android.text.TextPaint
import android.text.style.CharacterStyle
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.utils.ColorUtils
import java.util.regex.Pattern

class SpoilerPlugin : AbstractMarkwonPlugin() {
    override fun beforeSetText(textView: TextView, markdown: Spanned) {
        applySpoilerSpans(markdown as Spannable)
    }

    private class RedditSpoilerSpan : CharacterStyle() {
        private var revealed = false
        override fun updateDrawState(tp: TextPaint) {
            if (!revealed) {
                // use the same text color
                tp.bgColor = Color.DKGRAY
                tp.color = Color.DKGRAY
            } else {
                // for example keep a bit of black background to remind that it is a spoiler
                tp.bgColor = ColorUtils.applyAlpha(Color.DKGRAY, 25)
            }
        }

        fun setRevealed(revealed: Boolean) {
            this.revealed = revealed
        }
    }

    // we also could make text size smaller (but then MetricAffectingSpan should be used)
    private class HideSpoilerSyntaxSpan : CharacterStyle() {
        override fun updateDrawState(tp: TextPaint) {
            // set transparent color
            tp.color = 0
        }
    }

    companion object {
        private val RE = Pattern.compile("~!.+?!~")
        private fun applySpoilerSpans(spannable: Spannable) {
            val text = spannable.toString()
            val matcher = RE.matcher(text)
            while (matcher.find()) {
                val spoilerSpan = RedditSpoilerSpan()
                val clickableSpan: ClickableSpan = object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        spoilerSpan.setRevealed(true)
                        widget.postInvalidateOnAnimation()
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        // no op
                    }
                }
                val s = matcher.start()
                val e = matcher.end()
                spannable.setSpan(spoilerSpan, s, e, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannable.setSpan(clickableSpan, s, e, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

                // we also can hide original syntax
                spannable.setSpan(
                    HideSpoilerSyntaxSpan(),
                    s,
                    s + 2,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                spannable.setSpan(
                    HideSpoilerSyntaxSpan(),
                    e - 2,
                    e,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
    }
}
