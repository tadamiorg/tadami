package com.sf.tadami.ui.main.onboarding.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sf.tadami.R
import com.sf.tadami.ui.utils.padding

internal class GuidesStep(
    private val onRestoreBackup: () -> Unit,
) : OnboardingStep {

    override val isComplete: Boolean = true

    @Composable
    override fun Content() {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
        ) {
            Text(stringResource(R.string.onboarding_guides_returning_user, stringResource(R.string.app_name)))
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onRestoreBackup,
            ) {
                Text(stringResource(R.string.preferences_backup_restore_title))
            }
        }
    }
}
