package com.sairen.player.ui.theme

import com.sairen.player.data.ThemeMode

/** Pure resolution of the user's preference, so it can be unit tested. */
fun shouldUseDarkTheme(mode: ThemeMode, systemInDarkTheme: Boolean): Boolean = when (mode) {
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
    ThemeMode.SYSTEM -> systemInDarkTheme
}
