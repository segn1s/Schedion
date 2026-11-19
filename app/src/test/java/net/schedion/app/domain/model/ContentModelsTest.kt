package net.schedion.app.domain.model

import org.junit.Assert.*
import org.junit.Test

class ContentModelsTest {
    @Test fun urls_supportRelativeFilesButRejectExecutableSchemes() {
        assertEquals("https://schedion.ru/files/photo.webp", contentUrl("/files/photo.webp"))
        assertEquals("https://example.org/photo", contentUrl("https://example.org/photo"))
        assertNull(contentUrl("javascript:alert(1)"))
        assertNull(contentUrl("file:///etc/passwd"))
        assertNull(contentUrl("data:image/png;base64,abc"))
        assertNull(contentUrl("http://"))
        assertNull(contentUrl(""))
        assertNull(articleLink("intent://other"))
        assertEquals("tg://resolve?domain=test", articleLink("tg://resolve?domain=test"))
    }

    @Test fun fileTypes_supportOfficeAndPdfIncludingGenericServerMime() {
        assertEquals("application/pdf", fileMimeType("PDF", "file"))
        assertEquals("application/msword", fileMimeType("", "file.DOC"))
        assertEquals("application/vnd.openxmlformats-officedocument.wordprocessingml.document", fileMimeType("docx", "file"))
        assertEquals("application/vnd.ms-excel", fileMimeType("application/octet-stream", "file.xls"))
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", fileMimeType("xlsx", "file"))
        assertEquals("application/octet-stream", fileMimeType("", "file.unknown"))
        assertEquals("application/pdf", fileMimeType("Document", "file.pdf"))
        assertEquals("application/pdf", fileMimeType("application/pdf; charset=binary", "file"))
    }

    @Test fun fileNames_cannotEscapeDownloadsDirectory() {
        assertEquals("file.pdf", safeFileName("../../file.pdf"))
        assertEquals("file.pdf", safeFileName("..\\..\\file.pdf"))
        assertFalse(safeFileName("bad:name?.pdf").contains(":"))
        assertEquals("document", safeFileName(".."))
        assertTrue(safeFileName("x".repeat(500)).length <= 120)
    }

    @Test fun versionAndUrl_changesProduceDistinctDownloadKeys() {
        val file = ContentFile("document:one", "v1", "https://schedion.ru/files/a", "one.pdf", "application/pdf", 10)
        assertEquals(file.key, file.copy().key)
        assertNotEquals(file.key, file.copy(version = "v2").key)
        assertNotEquals(file.key, file.copy(url = "https://schedion.ru/files/b").key)
        assertNotEquals(file.key, file.copy(id = "document:two").key)
    }
}
