package net.schedion.app.ui.screens.news

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArticleHtmlParserTest {
    @Test fun headingsEntitiesQuotesAndInlineFormatting_arePreserved() {
        val blocks = parseArticleHtml("<h2>Heading</h2><p>A &amp; B <b>bold</b> <i>italic</i></p><blockquote>Quote</blockquote>")
        val text = blocks.filterIsInstance<ArticleBlock.Text>()
        assertTrue(text.first().heading)
        assertTrue(text.any { it.runs.any { run -> "A & B" in run.text } })
        assertTrue(text.any { it.runs.any { run -> run.text == "bold" && run.bold } })
        assertTrue(text.any { it.runs.any { run -> run.text == "italic" && run.italic } })
        assertTrue(text.last().quote)
    }

    @Test fun lists_preserveNumberingAndNestedItems() {
        val text = parseArticleHtml("<ol><li>First<ul><li>Nested</li></ul></li><li>Second</li></ol>")
            .filterIsInstance<ArticleBlock.Text>().joinToString("\n") { block -> block.runs.joinToString("") { it.text } }
        assertTrue(text, "1. First" in text)
        assertTrue(text, "• Nested" in text)
        assertTrue(text, "2. Second" in text)
    }

    @Test fun imagesAndRelativeLinks_keepOrderWithoutUnsafeSchemes() {
        val blocks = parseArticleHtml("<p>Before</p><img src='/files/photo'><p><a href='/files/pdf'>File</a> <a href='javascript:alert(1)'>Bad</a></p>")
        val imageIndex = blocks.indexOfFirst { it is ArticleBlock.Image }
        assertTrue(imageIndex > 0 && imageIndex < blocks.lastIndex)
        assertEquals("https://schedion.ru/files/photo", (blocks[imageIndex] as ArticleBlock.Image).url)
        val runs = blocks.filterIsInstance<ArticleBlock.Text>().flatMap { it.runs }
        assertTrue(runs.any { it.text == "File" && it.link == "https://schedion.ru/files/pdf" })
        assertTrue(runs.none { it.link?.startsWith("javascript:") == true })
    }

    @Test fun malformedHtmlAndScripts_doNotBreakRendering() {
        val blocks = parseArticleHtml("<script>alert('x')</script><style>p{color:red}</style><p>One<b>Two<p>Three")
        val text = blocks.filterIsInstance<ArticleBlock.Text>().flatMap { it.runs }.joinToString("") { it.text }
        assertTrue(text, "One" in text && "Two" in text && "Three" in text)
        assertFalse(text, "alert" in text || "color:red" in text)
    }
}
