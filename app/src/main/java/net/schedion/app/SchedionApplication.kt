package net.schedion.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Класс приложения, инициализирующий граф зависимостей Hilt для всего процесса.
 */
@HiltAndroidApp
class SchedionApplication : Application()
