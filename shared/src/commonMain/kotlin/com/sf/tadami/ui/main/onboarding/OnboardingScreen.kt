package com.sf.tadami.ui.main.onboarding

import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.Injekt
import com.sf.tadami.ui.PlatformUi
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import org.jetbrains.compose.resources.stringResource
import com.sf.tadami.shared.resources.Res
import com.sf.tadami.shared.resources.*
import com.sf.tadami.ui.main.onboarding.steps.GuidesStep
import com.sf.tadami.ui.main.onboarding.steps.StorageStep
import com.sf.tadami.ui.main.onboarding.steps.ThemeStep
import com.sf.tadami.ui.utils.padding
import soup.compose.material.motion.animation.materialSharedAxisX
import soup.compose.material.motion.animation.rememberSlideDistance

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    onRestoreBackup: () -> Unit,
) {
    val slideDistance = rememberSlideDistance()

    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    val steps = remember {
        listOfNotNull(
            ThemeStep(),
            StorageStep(),
            Injekt.get<PlatformUi>().permissionStep(),
            GuidesStep(onRestoreBackup)
        )
    }
    val isLastStep = currentStep == steps.lastIndex
    val closeApp = Injekt.get<PlatformUi>().rememberCloseApp()

    BackHandler(enabled = currentStep != 0, onBack = { currentStep-- })

    BackHandler(enabled = currentStep == 0, onBack = {
        closeApp()
    })

    InfoScreen(
        icon = Icons.Outlined.RocketLaunch,
        headingText = stringResource(Res.string.onboarding_heading),
        subtitleText = stringResource(Res.string.onboarding_description),
        acceptText = stringResource(
            if (isLastStep) {
                Res.string.onboarding_action_finish
            } else {
                Res.string.onboarding_action_next
            },
        ),
        canAccept = steps[currentStep].isComplete,
        onAcceptClick = {
            if (isLastStep) {
                onComplete()
            } else {
                currentStep++
            }
        },
    ) {
        Box(
            modifier = Modifier
                .padding(vertical = MaterialTheme.padding.small)
                .clip(MaterialTheme.shapes.small)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    materialSharedAxisX(
                        forward = targetState > initialState,
                        slideDistance = slideDistance,
                    )
                },
                label = "stepContent",
            ) {
                steps[it].Content()
            }
        }
    }
}