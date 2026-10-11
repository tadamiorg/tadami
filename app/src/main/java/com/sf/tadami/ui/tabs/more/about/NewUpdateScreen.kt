package com.sf.tadami.ui.tabs.more.about

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import com.halilibo.richtext.commonmark.CommonmarkAstNodeParser
import com.halilibo.richtext.markdown.BasicMarkdown
import com.halilibo.richtext.ui.BlockQuoteGutter
import com.halilibo.richtext.ui.RichTextStyle
import com.halilibo.richtext.ui.material3.RichText
import com.halilibo.richtext.ui.string.RichTextStringStyle
import com.sf.tadami.R
import com.sf.tadami.data.update.GithubUpdate
import com.sf.tadami.notifications.appupdate.AppUpdateWorker
import com.sf.tadami.ui.main.onboarding.InfoScreen
import com.sf.tadami.ui.utils.padding

/**
 * Full screen shown when a newer release is out: its changelog, then Download or Not now.
 * [onDismiss] closes it, also after Download since the download goes on in a notification.
 */
@Composable
fun NewUpdateScreen(
    release: GithubUpdate,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    InfoScreen(
        icon = Icons.Outlined.NewReleases,
        headingText = stringResource(id = R.string.update_check_new_version),
        subtitleText = "${stringResource(id = R.string.app_name)} ${release.version}",
        acceptText = stringResource(id = R.string.download),
        onAcceptClick = {
            val downloadLink = release.getDownloadLink()
            // No APK attached to the release: let the user get it from the release page instead
            if (downloadLink != null) {
                AppUpdateWorker.startNow(context, downloadLink)
            } else {
                uriHandler.openUri(release.releaseLink)
            }
            onDismiss()
        },
        rejectText = stringResource(id = R.string.action_not_now),
        onRejectClick = onDismiss,
    ) {
        val blockQuotesColor = MaterialTheme.colorScheme.primary
        RichText(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = MaterialTheme.padding.large),
            style = RichTextStyle(
                blockQuoteGutter = BlockQuoteGutter.BarGutter(
                    color = { blockQuotesColor.copy(alpha = .55f) }
                ),
                stringStyle = RichTextStringStyle(
                    linkStyle = TextLinkStyles(style = SpanStyle(color = MaterialTheme.colorScheme.primary)),
                ),
            ),
        ) {
            val parser = remember { CommonmarkAstNodeParser() }
            val astNode = remember(parser, release.info) {
                parser.parse(release.info)
            }
            BasicMarkdown(astNode)
        }
        TextButton(
            onClick = { uriHandler.openUri(release.releaseLink) },
            modifier = Modifier.padding(vertical = MaterialTheme.padding.small),
        ) {
            Text(text = stringResource(id = R.string.update_check_open_on_github))
            Spacer(modifier = Modifier.width(MaterialTheme.padding.extraSmall))
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                contentDescription = null,
            )
        }
    }
}
