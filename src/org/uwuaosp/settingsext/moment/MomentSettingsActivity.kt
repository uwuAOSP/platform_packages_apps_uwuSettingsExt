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
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.airbnb.lottie.LottieAnimationView
import org.uwuaosp.compose.settingslib.MainSwitchPreference
import org.uwuaosp.compose.settingslib.PrimarySwitchPreferenceRow
import org.uwuaosp.compose.settingslib.PreferenceRow
import org.uwuaosp.compose.settingslib.SettingsHomepageIcon
import org.uwuaosp.compose.settingslib.SettingsScaffold
import org.uwuaosp.compose.settingslib.SettingsSection
import org.uwuaosp.compose.settingslib.SettingsToolbarActionButton
import org.uwuaosp.compose.settingslib.SwitchPreferenceRow
import org.uwuaosp.compose.settingslib.rememberSettingsTypography
import org.uwuaosp.settingsext.R

class MomentSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MomentSettingsTheme {
                MomentSettingsScreen(onNavigateUp = ::finish)
            }
        }
    }
}

@Composable
private fun MomentSettingsTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()
    val colorScheme =
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    MaterialTheme(
        colorScheme = colorScheme,
        typography = rememberSettingsTypography(),
        content = content,
    )
}

@Composable
private fun MomentSettingsScreen(onNavigateUp: () -> Unit) {
    val context = LocalContext.current
    var momentEnabled by remember {
        mutableStateOf(MomentSecureSettings.isEnabled(context, false))
    }
    var arcGestureEnabled by remember {
        mutableStateOf(MomentSecureSettings.isArcGestureEnabled(context, true))
    }
    var navHandleDoubleTapEnabled by remember {
        mutableStateOf(MomentSecureSettings.isNavHandleDoubleTapEnabled(context, true))
    }
    var notificationClickEnabled by remember {
        mutableStateOf(MomentSecureSettings.isNotificationClickEnabled(context, true))
    }
    var notificationClickPortraitEnabled by remember {
        mutableStateOf(MomentSecureSettings.isNotificationClickPortraitEnabled(context, true))
    }
    var notificationClickLandscapeEnabled by remember {
        mutableStateOf(MomentSecureSettings.isNotificationClickLandscapeEnabled(context, true))
    }
    val navHandleDoubleTapSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        navHandleDoubleTapEnabled =
            MomentSecureSettings.isNavHandleDoubleTapEnabled(context, true)
    }

    SettingsScaffold(
        title = stringResource(R.string.moment_settings_title),
        showBackButton = true,
        onNavigateUp = onNavigateUp,
    ) {
        MomentSettingsIllustration()
        MainSwitchPreference(
            title = stringResource(R.string.moment_enabled_title),
            checked = momentEnabled,
            onCheckedChange = { enabled ->
                momentEnabled = enabled
                MomentSecureSettings.setEnabled(context, enabled)
            },
        )

        SettingsSection(title = stringResource(R.string.moment_gestures_section)) {
            item {
                PrimarySwitchPreferenceRow(
            title = stringResource(R.string.moment_nav_handle_double_tap_title),
            summary = stringResource(R.string.moment_nav_handle_double_tap_summary),
            checked = navHandleDoubleTapEnabled,
            onCheckedChange = { enabled ->
                navHandleDoubleTapEnabled = enabled
                MomentSecureSettings.setNavHandleDoubleTapEnabled(context, enabled)
            },
            onClick = {
                navHandleDoubleTapSettingsLauncher.launch(
                    Intent(context, NavHandleDoubleTapSettingsActivity::class.java),
                )
            },
            enabled = momentEnabled,
                )
            }
            item {
                PrimarySwitchPreferenceRow(
            title = stringResource(R.string.moment_arc_gesture_title),
            summary = stringResource(R.string.moment_arc_gesture_summary),
            checked = arcGestureEnabled,
            onCheckedChange = { enabled ->
                arcGestureEnabled = enabled
                MomentSecureSettings.setArcGestureEnabled(context, enabled)
            },
            onClick = {
                context.startActivity(Intent(context, MomentArcEditorActivity::class.java))
            },
            enabled = momentEnabled,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        SettingsSection(title = stringResource(R.string.moment_notification_section)) {
            item {
                SwitchPreferenceRow(
            title = stringResource(R.string.moment_notification_click_title),
            summary = stringResource(R.string.moment_notification_click_summary),
            checked = notificationClickEnabled,
            onCheckedChange = { enabled ->
                notificationClickEnabled = enabled
                MomentSecureSettings.setNotificationClickEnabled(context, enabled)
            },
            enabled = momentEnabled,
                )
            }
            item {
                SwitchPreferenceRow(
            title = stringResource(R.string.moment_notification_click_portrait_title),
            summary = "",
            showSummary = false,
            checked = notificationClickPortraitEnabled,
            onCheckedChange = { enabled ->
                notificationClickPortraitEnabled = enabled
                MomentSecureSettings.setNotificationClickPortraitEnabled(context, enabled)
            },
            enabled = momentEnabled && notificationClickEnabled,
                )
            }
            item {
                SwitchPreferenceRow(
            title = stringResource(R.string.moment_notification_click_landscape_title),
            summary = "",
            showSummary = false,
            checked = notificationClickLandscapeEnabled,
            onCheckedChange = { enabled ->
                notificationClickLandscapeEnabled = enabled
                MomentSecureSettings.setNotificationClickLandscapeEnabled(context, enabled)
            },
            enabled = momentEnabled && notificationClickEnabled,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        SettingsSection(title = stringResource(R.string.moment_section_experience)) {
            item {
                PreferenceRow(
            title = stringResource(R.string.moment_launch_title),
            summary = stringResource(R.string.moment_launch_summary),
            iconContent = {
                SettingsHomepageIcon(iconRes = R.drawable.ic_moment)
            },
            enabled = momentEnabled,
            onClick = {
                MomentAllAppsActivity.startInMoment(context)
            },
                )
            }
        }
    }
}

@Composable
private fun MomentSettingsIllustration() {
    val animations = listOf(
        MomentTutorial(
            animationRes = R.raw.moment_tutorial_fullscreen,
            title = stringResource(R.string.moment_tutorial_fullscreen_title),
            description = stringResource(R.string.moment_tutorial_fullscreen_description),
        ),
        MomentTutorial(
            animationRes = R.raw.moment_tutorial_close,
            title = stringResource(R.string.moment_tutorial_close_title),
            description = stringResource(R.string.moment_tutorial_close_description),
        ),
    )
    val pagerState = rememberPagerState { animations.size }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(40.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                beyondViewportPageCount = 1,
            ) { page ->
                MomentSettingsTutorialPage(
                    tutorial = animations[page],
                    isActive = pagerState.currentPage == page,
                )
            }
        }
        LauncherPageIndicator(
            pageCount = animations.size,
            currentPage = pagerState.currentPage,
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
        )
    }
}

private data class MomentTutorial(
    val animationRes: Int,
    val title: String,
    val description: String,
)

@Composable
private fun LauncherPageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        repeat(pageCount) { page ->
            Box(
                modifier = Modifier
                    .size(width = if (page == currentPage) 12.dp else 6.dp, height = 6.dp)
                    .alpha(if (page == currentPage) 1f else 0.5f)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                    ),
            )
            if (page != pageCount - 1) Spacer(modifier = Modifier.size(width = 4.dp, height = 1.dp))
        }
    }
}

@Composable
private fun MomentSettingsTutorialPage(
    tutorial: MomentTutorial,
    isActive: Boolean,
) {
    val context = LocalContext.current
    var animationFinished by remember(tutorial.animationRes) { mutableStateOf(false) }
    val animationView = remember(context, tutorial.animationRes) {
        LottieAnimationView(context).apply {
            setAnimation(tutorial.animationRes)
            repeatCount = 0
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = tutorial.description
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

    LaunchedEffect(animationView, isActive) {
        if (isActive && !animationFinished) {
            animationView.resumeAnimation()
        } else if (!isActive) {
            animationView.pauseAnimation()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp),
    ) {
        val contentWidth = (maxWidth - 32.dp).coerceAtLeast(0.dp)
        val animationPaneWidth = contentWidth * 0.56f
        val textPaneWidth = contentWidth - animationPaneWidth
        val animationWidth = minOf(
            (animationPaneWidth - 61.dp).coerceAtLeast(80.dp),
            300.dp * 825f / 1451f,
        )
        val textPaneOverlap = ((animationPaneWidth - animationWidth) / 2f - 2.5.dp)
            .coerceAtLeast(0.dp)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(
                -textPaneOverlap,
            ),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .size(width = animationPaneWidth, height = 336.dp),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(336.dp),
                    contentAlignment = androidx.compose.ui.Alignment.Center,
                ) {
                    val animationHeight = animationWidth * 1451f / 825f
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.Bottom,
                    ) {
                        AndroidView(
                            factory = { animationView },
                            modifier = Modifier.size(
                                width = animationWidth,
                                height = animationHeight,
                            ),
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Box(
                            modifier = Modifier
                                .size(56.dp),
                            contentAlignment = androidx.compose.ui.Alignment.Center,
                        ) {
                            if (animationFinished) {
                                SettingsToolbarActionButton(
                                    imageVector = ImageVector.vectorResource(
                                        R.drawable.ic_replay_animation,
                                    ),
                                    contentDescription = stringResource(R.string.replay_animation),
                                    onClick = {
                                        animationFinished = false
                                        animationView.progress = 0f
                                        animationView.playAnimation()
                                    },
                                )
                            }
                        }
                    }
                }
            }
            Column(
                modifier = Modifier
                    .width(textPaneWidth + textPaneOverlap)
                    .fillMaxHeight()
                    .padding(end = 8.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            ) {
                Text(
                    text = tutorial.title,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = tutorial.description,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun MomentArcEditorIcon() {
    SettingsHomepageIcon(iconRes = R.drawable.ic_moment_arc_edit)
}
