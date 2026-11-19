package net.schedion.app.ui.screens.news

import android.graphics.Typeface
import android.text.Html
import android.text.Editable
import java.util.ArrayDeque
import org.xml.sax.XMLReader
import android.text.style.*
import net.schedion.app.domain.model.articleLink
import net.schedion.app.domain.model.contentUrl

sealed interface ArticleBlock {
    data class Text(val runs: List<ArticleRun>, val heading: Boolean, val quote: Boolean, val bullet: Boolean) : ArticleBlock
    data class Image(val url: String) : ArticleBlock
}
data class ArticleRun(val text: String, val bold: Boolean = false, val italic: Boolean = false, val link: String? = null)

/** Использует системный HTML-парсер, а результат преобразует в блоки нативного Compose без WebView. */
internal fun parseArticleHtml(html: String): List<ArticleBlock> {
    val sanitized = html.replace(Regex("<(script|style)\\b[^>]*>.*?</\\1\\s*>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
        .replace(Regex("<(\\/?)(ol|ul|li)\\b[^>]*>", RegexOption.IGNORE_CASE)) { match ->
            "<${match.groupValues[1]}schedion${match.groupValues[2].lowercase()}>"
        }
    val text = Html.fromHtml(sanitized, Html.FROM_HTML_MODE_COMPACT, null, ArticleListHandler())
    val result = mutableListOf<ArticleBlock>()
    var start = 0
    while (start < text.length) {
        val image = text.getSpans(start, start + 1, ImageSpan::class.java).firstOrNull()
        if (image != null) {
            contentUrl(image.source)?.let { result.add(ArticleBlock.Image(it)) }
            start = maxOf(start + 1, text.getSpanEnd(image))
            continue
        }
        var end = start
        while (end < text.length && text[end] != '\n' && text.getSpans(end, end + 1, ImageSpan::class.java).isEmpty()) end++
        if (end > start && text.subSequence(start, end).isNotBlank()) {
            val runs = mutableListOf<ArticleRun>()
            var index = start
            while (index < end) {
                val boundary = text.nextSpanTransition(index, end, Any::class.java).coerceAtLeast(index + 1)
                val styles = text.getSpans(index, boundary, StyleSpan::class.java).map { it.style }
                val link = text.getSpans(index, boundary, URLSpan::class.java).firstOrNull()?.url?.let(::articleLink)
                runs.add(ArticleRun(text.subSequence(index, boundary).toString(),
                    styles.any { it == Typeface.BOLD || it == Typeface.BOLD_ITALIC },
                    styles.any { it == Typeface.ITALIC || it == Typeface.BOLD_ITALIC }, link))
                index = boundary
            }
            result.add(ArticleBlock.Text(runs,
                text.getSpans(start, end, RelativeSizeSpan::class.java).any { it.sizeChange > 1f },
                text.getSpans(start, end, QuoteSpan::class.java).isNotEmpty(),
                text.getSpans(start, end, BulletSpan::class.java).isNotEmpty()))
        }
        start = if (end < text.length && text[end] == '\n') end + 1 else maxOf(end, start + 1)
    }
    return result
}

/** Нативный Html не сохраняет нумерацию ol; неизвестные теги списков обрабатываются отдельно. */
private class ArticleListHandler : Html.TagHandler {
    private data class Level(val ordered: Boolean, var number: Int = 0)
    private val levels = ArrayDeque<Level>()
    override fun handleTag(opening: Boolean, tag: String, output: Editable, reader: XMLReader) {
        fun newLine() { if (output.isNotEmpty() && output.last() != '\n') output.append('\n') }
        when (tag) {
            "schedionol", "schedionul" -> {
                newLine()
                if (opening) levels.addLast(Level(tag == "schedionol"))
                else if (levels.isNotEmpty()) levels.removeLast()
            }
            "schedionli" -> {
                newLine()
                if (opening) {
                    val level = levels.peekLast()
                    output.append("  ".repeat((levels.size - 1).coerceAtLeast(0)))
                    if (level?.ordered == true) output.append("${++level.number}. ")
                    else output.append("• ")
                }
            }
        }
    }
}
