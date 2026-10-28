/*
 * Copyright (C) 2026 The uwuAOSP Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.uwuaosp.settingsext.moment

import android.animation.Animator
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.airbnb.lottie.LottieAnimationView
import org.uwuaosp.compose.widget.preference.MainSwitchPreference
import org.uwuaosp.compose.widget.scaffold.SettingsScaffold
import org.uwuaosp.compose.widget.scaffold.SettingsToolbarActionButton
import org.uwuaosp.compose.widget.ui.SettingsTopIntro
import org.uwuaosp.settingsext.R
import org.uwuaosp.settingsext.SettingsExtTheme

class NavHandleDoubleTapSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SettingsExtTheme {
                NavHandleDoubleTapSettingsScreen(onNavigateUp = ::finish)
            }
        }
    }
}

@Composable
private fun NavHandleDoubleTapSettingsScreen(onNavigateUp: () -> Unit) {
    val context = LocalContext.current
    var enabled by remember {
        mutableStateOf(MomentSecureSettings.isNavHandleDoubleTapEnabled(context, true))
    }

    SettingsScaffold(
        title = stringResource(R.string.moment_nav_handle_double_tap_title),
        showBackButton = true,
        onNavigateUp = onNavigateUp,
        contentTopPadding = 0.dp,
    ) {
        SettingsTopIntro(
            text = stringResource(R.string.moment_nav_handle_double_tap_description),
            modifier = Modifier.padding(bottom = 16.dp),
        )
        NavHandleDoubleTapIllustration()
        MainSwitchPreference(
            title = stringResource(R.string.moment_nav_handle_double_tap_switch_title),
            checked = enabled,
            onCheckedChange = { newValue ->
                enabled = newValue
                MomentSecureSettings.setNavHandleDoubleTapEnabled(context, newValue)
            },
        )
    }
}

@Composable
private fun NavHandleDoubleTapIllustration() {
    val context = LocalContext.current
    val description = stringResource(R.string.moment_nav_handle_double_tap_animation_description)
    var animationFinished by remember { mutableStateOf(false) }
    val animationView = remember(context) {
        LottieAnimationView(context).apply {
            setAnimation(R.raw.moment_nav_handle_double_tap)
            repeatCount = 0
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = description
            playAnimation()
        }
    }

    DisposableEffect(animationView) {
        val listener = object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) = Unit

            override fun onAnimationEnd(animation: Animator) {
                animationFinished = true
            }

            override fun onAnimationCancel(animation: Animator) = Unit

            override fun onAnimationRepeat(animation: Animator) = Unit
        }
        animationView.addAnimatorListener(listener)
        onDispose {
            animationView.removeAnimatorListener(listener)
            animationView.cancelAnimation()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp),
        contentAlignment = Alignment.Center,
    ) {
        val animationWidth = minOf(
            280.dp,
            (maxWidth - 112.dp).coerceAtLeast(160.dp),
            420.dp * 825f / 1428f,
        )
        val animationHeight = animationWidth * 1428f / 825f

        AndroidView(
            factory = { animationView },
            modifier = Modifier
                .size(width = animationWidth, height = animationHeight)
                .align(Alignment.Center),
        )
        if (animationFinished) {
            SettingsToolbarActionButton(
                imageVector = ImageVector.vectorResource(R.drawable.ic_replay_animation),
                contentDescription = stringResource(R.string.replay_animation),
                onClick = {
                    animationFinished = false
                    animationView.progress = 0f
                    animationView.playAnimation()
                },
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(
                        x = animationWidth / 2 + 28.dp,
                        y = animationHeight / 2 - 28.dp,
                    ),
            )
        }
    }
}
