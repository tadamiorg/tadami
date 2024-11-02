package com.sf.tadami.navigation.graphs.onboarding

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.sf.tadami.navigation.graphs.settings.SettingsRoutes
import com.sf.tadami.preferences.app.BasePreferences
import com.sf.tadami.preferences.model.DataStoreState
import com.sf.tadami.ui.main.onboarding.OnboardingScreen

fun NavGraphBuilder.onboardingNavGraph(
    navController: NavHostController,
    basePreferencesState: DataStoreState<BasePreferences>
) {
    composable(
        route = OnboardingRoutes.ONBOARDING
    ) {
        OnboardingScreen(
            onComplete = {
                basePreferencesState.setValue(BasePreferences(onboardingComplete = true))
                navController.popBackStack()
            },
            onRestoreBackup = {
                basePreferencesState.setValue(BasePreferences(onboardingComplete = true))
                navController.navigate(SettingsRoutes.DATA) {
                    popUpTo(navController.graph.findStartDestination().id)
                }
            }
        )
    }
}

object OnboardingRoutes {
    const val ONBOARDING = "onboarding_screen"
}