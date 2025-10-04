package com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.episodetooltip

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.halilibo.richtext.commonmark.CommonmarkAstNodeParser
import com.halilibo.richtext.markdown.BasicMarkdown
import com.halilibo.richtext.ui.BlockQuoteGutter
import com.halilibo.richtext.ui.RichTextStyle
import com.halilibo.richtext.ui.material3.RichText
import com.halilibo.richtext.ui.string.RichTextStringStyle
import com.sf.tadami.R
import com.sf.tadami.preferences.CommonKeys
import com.sf.tadami.preferences.model.rememberUnknownDataStoreState
import com.sf.tadami.ui.components.filters.CheckBox
import com.sf.tadami.ui.components.screens.ScreenTabContent
import com.sf.tadami.ui.components.widgets.FastScrollLazyColumn
import com.sf.tadami.ui.utils.padding

@Composable()
fun tooltipTab(
    sourceDatastore: DataStore<Preferences>,
    tooltipContent: String,
): ScreenTabContent {
    val datastoreState = rememberUnknownDataStoreState(unknownDataStore = sourceDatastore)
    val sourcePreferences by datastoreState.value.collectAsState()
    val listState = rememberLazyListState()
    val blockQuotescolor = MaterialTheme.colorScheme.primary
    var fabHeight by remember {
        mutableIntStateOf(0)
    }

    val heightInDp = with(LocalDensity.current) { fabHeight.toDp() }

    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }

    return ScreenTabContent(
        titleRes = R.string.player_epsiode_tooltip_title,
    ) { contentPadding: PaddingValues, _ ->
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            floatingActionButton = {
                FloatingActionButton(
                    interactionSource = interactionSource,
                    modifier = Modifier.onGloballyPositioned {
                        fabHeight = it.size.height
                    },
                    onClick = {
                        datastoreState.setValue(
                            (sourcePreferences[CommonKeys.SHOULD_SHOW_EPISODE_TOOLTIP]
                                ?: true).not(), CommonKeys.SHOULD_SHOW_EPISODE_TOOLTIP
                        )
                    },
                   content = {
                       CheckBox(
                           textModifier = Modifier.padding(end = MaterialTheme.padding.small),
                           interactionSource = interactionSource,
                           maxWidth = false,
                           title = stringResource(R.string.player_epsiode_tooltip_show_again),
                           state = (sourcePreferences[CommonKeys.SHOULD_SHOW_EPISODE_TOOLTIP]
                               ?: true).not()
                       ) { checked ->
                           datastoreState.setValue(
                               checked.not(),
                               CommonKeys.SHOULD_SHOW_EPISODE_TOOLTIP
                           )
                       }
                   }
                )
            },
            floatingActionButtonPosition = FabPosition.End
        ) { padding ->
            FastScrollLazyColumn(
                thumbAlways = true,
                noEndPadding = true,
                state = listState,
                contentPadding = PaddingValues(bottom = heightInDp),
            ) {
                item {
                    RichText(
                            modifier = Modifier
                                .padding(
                                    MaterialTheme.padding.small
                                ),
                            style = RichTextStyle(
                                blockQuoteGutter = BlockQuoteGutter.BarGutter(
                                    color = { blockQuotescolor.copy(alpha = .55f) }
                                ),
                                stringStyle = RichTextStringStyle(

                                    linkStyle = TextLinkStyles(style = SpanStyle(MaterialTheme.colorScheme.primary)),
                                ),
                            ),
                        ) {
                            val parser = remember { CommonmarkAstNodeParser() }
                            val astNode = remember(parser) {
                                parser.parse(
                                    tooltipContent
                                )
                            }
                            BasicMarkdown(astNode)
                        }
                    }
            }
        }
    }
}

