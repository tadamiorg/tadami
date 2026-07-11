package com.sf.tadami.ui.animeinfos.episode.cast

import android.content.Context
import android.content.Intent
import com.google.android.gms.cast.framework.media.MediaIntentReceiver
import com.sf.tadami.notifications.cast.CastControlService

/**
 * Custom [MediaIntentReceiver] for the Cast media notification. The default skip-next/prev act on the
 * Cast queue (which we don't use); we intercept those two actions and switch **episodes** instead,
 * routing to [CastControlService] which re-resolves + loads the neighbour episode app-scoped (works
 * while the phone is locked / backgrounded). Every other transport action is delegated to [super] so
 * play/pause, rewind and forward keep their default behaviour.
 */
class CastMediaIntentReceiver : MediaIntentReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_SKIP_NEXT -> CastControlService.skipEpisode(context, forward = true)
            ACTION_SKIP_PREV -> CastControlService.skipEpisode(context, forward = false)
            else -> super.onReceive(context, intent)
        }
    }
}
