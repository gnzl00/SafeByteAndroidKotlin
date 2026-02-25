package com.safebyte

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

val LocalAppContext = staticCompositionLocalOf<Context> {
    error("No Context found")
}

@androidx.compose.runtime.Composable
fun ProvideAppContext(content: @androidx.compose.runtime.Composable () -> Unit) {
    val ctx = LocalContext.current.applicationContext
    androidx.compose.runtime.CompositionLocalProvider(LocalAppContext provides ctx, content = content)
}
