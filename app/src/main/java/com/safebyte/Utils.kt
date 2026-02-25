package com.safebyte

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun drawableId(resName: String): Int {
    val ctx = LocalContext.current
    return ctx.resources.getIdentifier(resName, "drawable", ctx.packageName)
}
