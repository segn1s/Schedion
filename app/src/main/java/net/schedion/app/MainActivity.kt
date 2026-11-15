package net.schedion.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import net.schedion.app.ui.navigation.AppNavGraph
import net.schedion.app.ui.screens.news.NewsViewModel
import net.schedion.app.ui.screens.schedule.ScheduleViewModel
import net.schedion.app.ui.theme.SchedionTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val scheduleViewModel: ScheduleViewModel by viewModels()
    private val newsViewModel: NewsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        setContent {
            SchedionTheme {
                AppNavGraph(
                    scheduleViewModel = scheduleViewModel,
                    newsViewModel = newsViewModel,
                )
            }
        }
    }
}
