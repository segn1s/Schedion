package net.schedion.app.ui.screens.people

import android.content.Intent
import android.graphics.Rect
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import net.schedion.app.R
import net.schedion.app.domain.model.FavoriteKind
import net.schedion.app.domain.model.FavoriteSource
import net.schedion.app.ui.theme.SchedionTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FavoritesListTest {
    @Test
    fun draggingHandleImmediatelyScrollsAndMovesSourceToEnd() {
        verifyDragToEnd(useHandle = true)
    }

    @Test
    fun holdingTileWithSmallFingerMovementScrollsAndMovesSourceToEnd() {
        verifyDragToEnd(useHandle = false)
    }

    private fun verifyDragToEnd(useHandle: Boolean) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val automation = instrumentation.uiAutomation
        val activity = instrumentation.startActivitySync(
            Intent(context, ComponentActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        ) as ComponentActivity
        val favorites = (1..10).map { id -> FavoriteSource(FavoriteKind.Group, id, "Группа $id") }
        val viewportBottom = AtomicReference<Float>()
        val viewportCenterX = AtomicReference<Float>()
        val receivedMove = CountDownLatch(1)
        val moves = mutableListOf<Pair<FavoriteSource, Int>>()

        try {
            instrumentation.runOnMainSync {
                activity.setContent {
                    SchedionTheme {
                        Box(Modifier.width(340.dp).height(310.dp).onGloballyPositioned {
                            val position = it.positionOnScreen()
                            viewportBottom.set(position.y + it.size.height)
                            viewportCenterX.set(position.x + it.size.width / 2f)
                        }) {
                            FavoritesList(
                                favorites = favorites,
                                limit = 10,
                                onBack = {},
                                onOpen = {},
                                onRemove = {},
                                onMove = { source, index ->
                                    moves.add(source to index)
                                    receivedMove.countDown()
                                },
                            )
                        }
                    }
                }
            }

            val label = context.getString(R.string.favorites_reorder, favorites.first().title)
            val handle = Rect()
            awaitCondition("Ручка первой строки видна") {
                automation.rootInActiveWindow?.findHandle(label)?.getBoundsInScreen(handle)
                !handle.isEmpty
            }

            val x = if (useHandle) handle.exactCenterX() else checkNotNull(viewportCenterX.get())
            val firstY = handle.exactCenterY()
            val bottom = checkNotNull(viewportBottom.get()) - 8f
            val downTime = SystemClock.uptimeMillis()
            fun touch(action: Int, y: Float) {
                val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, x, y, 0)
                try {
                    event.source = InputDevice.SOURCE_TOUCHSCREEN
                    assertTrue(automation.injectInputEvent(event, true))
                } finally {
                    event.recycle()
                }
            }

            touch(MotionEvent.ACTION_DOWN, firstY)
            try {
                if (!useHandle) {
                    touch(MotionEvent.ACTION_MOVE, firstY + 3f)
                    SystemClock.sleep(ViewConfiguration.getLongPressTimeout().toLong() + 250)
                }
                touch(MotionEvent.ACTION_MOVE, bottom)
                val lastHandle = context.getString(R.string.favorites_reorder, favorites.last().title)
                val moveDown = context.getString(R.string.favorites_move_down)
                awaitCondition("Исходно последняя строка появилась над перетаскиваемой") {
                    automation.rootInActiveWindow?.findHandle(lastHandle)?.actionList
                        ?.any { it.label?.toString() == moveDown } == true
                }
            } finally {
                touch(MotionEvent.ACTION_UP, bottom)
            }

            assertTrue("Перемещение передано на сохранение", receivedMove.await(5, TimeUnit.SECONDS))
            assertEquals(listOf(favorites.first() to favorites.lastIndex), moves)
        } finally {
            instrumentation.runOnMainSync { activity.finish() }
        }
    }

    private fun awaitCondition(message: String, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 12_000
        while (SystemClock.uptimeMillis() < deadline) {
            if (condition()) return
            SystemClock.sleep(50)
        }
        throw AssertionError(message)
    }

    private fun AccessibilityNodeInfo.findHandle(label: String): AccessibilityNodeInfo? {
        if (contentDescription?.toString() == label) return this
        return (0 until childCount).firstNotNullOfOrNull { getChild(it)?.findHandle(label) }
    }
}
