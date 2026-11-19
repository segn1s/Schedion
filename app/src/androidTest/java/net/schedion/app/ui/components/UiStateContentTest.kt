package net.schedion.app.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.atomic.AtomicInteger
import net.schedion.app.core.ContentState
import net.schedion.app.core.Resource
import net.schedion.app.ui.theme.SchedionTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UiStateContentTest {
    @get:Rule val compose = createComposeRule()

    @Test fun emptyContentStillSupportsPullToRefresh() {
        val refreshes = AtomicInteger()
        compose.setContent {
            SchedionTheme {
                PullToRefreshBox(false, { refreshes.incrementAndGet() }, Modifier.fillMaxSize()) {
                    UiStateContent(
                        ContentState(resource = Resource.Success(emptyList<String>())), {},
                        modifier = Modifier.fillMaxSize(), scrollFallback = true, isEmpty = { it.isEmpty() },
                    ) {}
                }
            }
        }
        compose.onRoot().performTouchInput { swipeDown(durationMillis = 600) }
        compose.runOnIdle { assertTrue("Pull-to-refresh must work on an empty screen", refreshes.get() > 0) }
    }
}
