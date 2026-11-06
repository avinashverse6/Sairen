package com.sairen.player

import android.app.Application

/**
 * Process entry point. Deliberately does almost nothing: a cached, dependency-free
 * Coil image loader would be created lazily by the library itself, and the music
 * library is only scanned once a screen asks for it.
 */
class SairenApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
