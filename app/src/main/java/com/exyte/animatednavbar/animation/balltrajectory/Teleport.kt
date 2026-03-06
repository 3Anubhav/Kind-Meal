package com.exyte.animatednavbar.animation.balltrajectory

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.exyte.animatednavbar.ballSize
import com.exyte.animatednavbar.utils.toPxf

@Stable
class Teleport(
    private val animationSpec: AnimationSpec<Float> = tween(durationMillis = 150)
) : BallAnimation {

    @Composable
    override fun animateAsState(targetOffset: Offset): State<BallAnimInfo> {
        if (targetOffset.isUnspecified) {
            return remember { mutableStateOf(BallAnimInfo()) }
        }

        val density = LocalDensity.current
        val verticalOffset = remember { 2.dp.toPxf(density) }
        val ballSizePx = remember { ballSize.toPxf(density) }

        var currentOffset by remember { mutableStateOf(targetOffset) }
        val scale = remember { Animatable(1f) }

        LaunchedEffect(targetOffset) {
            if (targetOffset != currentOffset) {
                scale.animateTo(0f, animationSpec)   // Disappear
                currentOffset = targetOffset         // Move
                scale.snapTo(0f)                     // Remain hidden at new position
                scale.animateTo(1f, animationSpec)   // Reappear
            }
        }

        return remember(scale.value, currentOffset) {
            mutableStateOf(
                BallAnimInfo(
                    scale = scale.value,
                    offset = Offset(
                        x = currentOffset.x - (ballSizePx / 2),
                        y = currentOffset.y - verticalOffset
                    )
                )
            )
        }
    }
}
