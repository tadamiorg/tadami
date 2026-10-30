package com.sf.tadami.ui.components.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.sf.tadami.R
import com.sf.tadami.navigation.graphs.settings.AdvancedSettingsRoutes
import com.sf.tadami.network.doh.DohProviders
import com.sf.tadami.network.doh.mayBeDnsBlock
import com.sf.tadami.preferences.advanced.AdvancedPreferences
import com.sf.tadami.preferences.model.rememberDataStoreState
import com.sf.tadami.ui.tabs.more.settings.components.PreferenceScreen
import com.sf.tadami.utils.isOnline

/** Whether to offer DNS over HTTPS for [error]: DoH is off, and a DNS blocking the host may cause it. */
@Composable
fun suggestsDoh(error: Throwable?): Boolean {
    val context = LocalContext.current
    val advancedPreferences by rememberDataStoreState(AdvancedPreferences).value.collectAsState()
    return error != null &&
        advancedPreferences.dohProvider == DohProviders.DISABLED &&
        error.mayBeDnsBlock() &&
        // Offline, no DNS helps
        context.isOnline()
}

/** Opens the advanced settings on the DNS over HTTPS setting, highlighted as from a settings search. */
@Composable
fun rememberOpenDohSetting(navController: NavController): () -> Unit {
    val title = stringResource(R.string.pref_dns_over_https)
    return remember(navController, title) {
        {
            PreferenceScreen.highlightKey = title
            navController.navigate(AdvancedSettingsRoutes.HOME)
        }
    }
}
