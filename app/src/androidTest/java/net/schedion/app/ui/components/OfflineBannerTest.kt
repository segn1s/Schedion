package net.schedion.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneId
import net.schedion.app.core.ContentState
import net.schedion.app.core.Resource
import net.schedion.app.ui.theme.SchedionTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfflineBannerTest {
    @get:Rule val compose = createComposeRule()

    @Test fun cachedContentHasOneCompactBannerWithTheSnapshotTime() {
        val savedAt = LocalDateTime.of(2026, 11, 18, 13, 45).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        compose.setContent {
            SchedionTheme {
                Column {
                    OfflineBanner(savedAt)
                    UiStateContent(
                        ContentState(resource = Resource.Success("Cached news", true, savedAt),
                            error = IOException(), savedAt = savedAt),
                        {}, showOfflineBanner = false,
                    ) { Text(it) }
                }
            }
        }
        compose.onNodeWithText("Cached news").assertIsDisplayed()
        compose.onNodeWithText("Нет сети").assertDoesNotExist()
        compose.onNodeWithText("Вне сети").assertIsDisplayed().assertHasNoClickAction()
        compose.onNodeWithText("Данные актуальны на 18.11.2026, 13:45").assertIsDisplayed()
        compose.onNodeWithText("Обновить").assertDoesNotExist()
    }

    @Test fun uncachedContentKeepsRetryOutsideTheBanner() {
        compose.setContent {
            SchedionTheme {
                Column {
                    OfflineBanner(savedAt = null)
                    UiStateContent<String>(
                        ContentState(resource = Resource.Error("", IOException()), error = IOException()),
                        {}, showOfflineBanner = false,
                    ) { Text(it) }
                }
            }
        }
        compose.onNodeWithText("Нет сети").assertDoesNotExist()
        compose.onNodeWithText("Повторить").assertIsDisplayed()
        compose.onNodeWithText("Вне сети").assertIsDisplayed().assertHasNoClickAction()
        compose.onNodeWithText("Данные актуальны на", substring = true).assertDoesNotExist()
    }
}
