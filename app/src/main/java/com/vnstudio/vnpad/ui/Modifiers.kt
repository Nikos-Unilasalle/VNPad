package com.vnstudio.vnpad.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/** Tap handling without the Material ripple — used on custom-drawn surfaces. */
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) }
