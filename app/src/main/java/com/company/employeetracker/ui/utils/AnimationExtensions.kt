package com.company.employeetracker.ui.utils

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun <T> T.animateOnChange(
    animationSpec: AnimationSpec<Float> = tween(300)
): T {
    return this
}

fun Modifier.fadeInAnimation(
    durationMillis: Int = 500
): Modifier {
    return this
}

fun slideInFromBottom(): EnterTransition {
    return slideInVertically(
        initialOffsetY = { it },
        animationSpec = tween(300, easing = FastOutSlowInEasing)
    ) + fadeIn(animationSpec = tween(300))
}

fun slideOutToBottom(): ExitTransition {
    return slideOutVertically(
        targetOffsetY = { it },
        animationSpec = tween(300, easing = FastOutSlowInEasing)
    ) + fadeOut(animationSpec = tween(300))
}

fun slideInFromTop(): EnterTransition {
    return slideInVertically(
        initialOffsetY = { -it },
        animationSpec = tween(300, easing = FastOutSlowInEasing)
    ) + fadeIn(animationSpec = tween(300))
}

fun slideInFromRight(): EnterTransition {
    return slideInHorizontally(
        initialOffsetX = { it },
        animationSpec = tween(300, easing = FastOutSlowInEasing)
    ) + fadeIn(animationSpec = tween(300))
}

fun scaleInEnter(): EnterTransition {
    return scaleIn(
        initialScale = 0.8f,
        animationSpec = tween(300, easing = FastOutSlowInEasing)
    ) + fadeIn(animationSpec = tween(300))
}

fun scaleOutExit(): ExitTransition {
    return scaleOut(
        targetScale = 0.8f,
        animationSpec = tween(300, easing = FastOutSlowInEasing)
    ) + fadeOut(animationSpec = tween(300))
}