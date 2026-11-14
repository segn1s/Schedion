package net.schedion.app.ui.screens.documents

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.PaddingValues
import net.schedion.app.ui.components.BackBar
import net.schedion.app.ui.components.Chip
import net.schedion.app.ui.components.Glyph
import net.schedion.app.ui.components.LineIcon
import net.schedion.app.ui.components.MockButton
import net.schedion.app.ui.components.MockCard
import net.schedion.app.ui.components.MockDivider
import net.schedion.app.ui.components.ScreenColumn
import net.schedion.app.ui.theme.SchedionColors as C
import net.schedion.app.ui.theme.SchedionDimens as D
import net.schedion.app.ui.theme.SchedionType as T

internal enum class FileType(val label: String, val color: Color) {
    Pdf("PDF", C.Pdf), Xlsx("XLSX", C.Xlsx), Docx("DOCX", C.Docx),
}

internal data class Document(
    val title: String,
    val meta: String,
    val type: FileType,
)

private val calendar = Document("Календарный учебный график 2026/27", "Документ PDF · 1,2 МБ", FileType.Pdf)
private val academicDocuments = listOf(
    calendar,
    Document("График консультаций преподавателей", "Таблица Excel · 48 КБ", FileType.Xlsx),
    Document("Положение о текущем контроле успеваемости", "Документ PDF · 640 КБ", FileType.Pdf),
)
private val forms = listOf(
    Document("Заявление на академический отпуск", "Документ Word · 32 КБ", FileType.Docx),
    Document("Заявление о переводе на другую форму обучения", "Документ Word · 29 КБ", FileType.Docx),
)

@Composable
internal fun DocumentsScreen(onOpenCalendar: () -> Unit, modifier: Modifier = Modifier) {
    ScreenColumn(modifier) {
        Text("Документы", style = T.Heading)
        Spacer(Modifier.height(D.L))
        Row(horizontalArrangement = Arrangement.spacedBy(D.Sm)) {
            listOf("Все", "Учебный процесс", "Заявления").forEachIndexed { index, label ->
                val active = index == 0
                Chip(
                    label,
                    if (active) C.Primary else C.Surface,
                    if (active) C.White else C.Primary,
                    outline = if (active) C.Primary else C.Outline,
                    padding = PaddingValues(horizontal = D.L, vertical = D.Sm),
                )
            }
        }
        Spacer(Modifier.height(D.L))
        DocumentSection("Учебный процесс", academicDocuments, onOpenCalendar)
        Spacer(Modifier.height(D.L))
        DocumentSection("Заявления", forms, onOpenCalendar)
    }
}

@Composable
private fun DocumentSection(label: String, documents: List<Document>, onOpenCalendar: () -> Unit) {
    Text(label, color = C.Secondary, style = T.LabelStrong)
    Spacer(Modifier.height(D.Sm))
    MockCard {
        documents.forEachIndexed { index, document ->
            if (index > 0) MockDivider()
            DocumentRow(document, onClick = onOpenCalendar.takeIf { document == calendar })
        }
    }
}

@Composable
internal fun DocumentRow(
    document: Document,
    modifier: Modifier = Modifier,
    attachment: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = if (attachment) D.FileHeight + D.Xxl else D.DocumentRow)
            .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick))
            .padding(horizontal = D.L, vertical = D.M),
        horizontalArrangement = Arrangement.spacedBy(D.Ml),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FileBadge(document.type)
        Column(Modifier.weight(1f)) {
            Text(document.title, style = T.Title)
            Spacer(Modifier.height(D.Tiny))
            Text(document.meta, color = C.Muted, style = T.Label)
        }
        if (attachment) {
            Text("Скачать", color = C.Primary, style = T.LabelStrong)
        } else {
            LineIcon(Glyph.Download, tint = C.Primary)
        }
    }
}

@Composable
private fun FileBadge(type: FileType, large: Boolean = false) {
    val shape = RoundedCornerShape(
        topStart = if (large) D.S else D.Xs,
        topEnd = if (large) D.Xl else D.Ml,
        bottomStart = if (large) D.S else D.Xs,
        bottomEnd = if (large) D.S else D.Xs,
    )
    Box(
        Modifier.size(
            width = if (large) D.LargeFileWidth else D.FileWidth,
            height = if (large) D.LargeFileHeight else D.FileHeight,
        ).background(type.color, shape).padding(bottom = if (large) D.Ml else D.S),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Text(type.label, color = C.White, style = if (large) T.LargeFile else T.File)
    }
}

@Composable
internal fun DocumentDetails(onBack: () -> Unit, modifier: Modifier = Modifier) {
    ScreenColumn(modifier) {
        BackBar("Документы", onBack)
        Spacer(Modifier.height(D.S))
        MockCard(large = true) {
            Column(
                Modifier.fillMaxWidth().heightIn(min = D.DocumentHero).padding(D.Xxl),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                FileBadge(FileType.Pdf, large = true)
                Spacer(Modifier.height(D.L))
                Text(calendar.title, style = T.SheetHeading, textAlign = TextAlign.Center)
                Spacer(Modifier.height(D.Page))
                Text(
                    "Сроки семестров, сессий, практик и каникул по курсам",
                    color = C.Secondary, style = T.Body, textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(D.Page))
        MockCard {
            Column(Modifier.padding(D.Page), verticalArrangement = Arrangement.spacedBy(D.Ml)) {
                DetailRow("Для кого") {
                    Column {
                        Chip("Студентам", C.PaleBlue, C.Primary)
                        Chip("Преподавателям", C.PaleBlue, C.Primary)
                    }
                }
                DetailRow("Файл") { Text("grafik-2026-27.pdf", style = T.Body) }
                DetailRow("Размер") { Text(calendar.meta, style = T.Body) }
                DetailRow("Обновлён") { Text("01.09.2026", style = T.Body) }
            }
        }
        Spacer(Modifier.height(D.Page))
        MockButton("Скачать", Modifier.fillMaxWidth(), primary = true, icon = Glyph.Download)
        Spacer(Modifier.height(D.Sm))
        MockButton("Поделиться ссылкой", Modifier.fillMaxWidth())
        Spacer(Modifier.height(D.Page))
        Text(
            "Ссылка постоянная: после замены файла она открывает новую версию",
            modifier = Modifier.fillMaxWidth(),
            color = C.Muted, style = T.Label, textAlign = TextAlign.Center,
        )
    }
}

@Composable
internal fun DetailRow(label: String, content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(label, Modifier.width(D.DetailsLabel), C.Muted, style = T.Body)
        Column(Modifier.weight(1f)) { content() }
    }
}
