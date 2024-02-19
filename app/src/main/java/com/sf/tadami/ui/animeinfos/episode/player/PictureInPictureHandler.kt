package com.sf.tadami.ui.animeinfos.episode.player

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Rational
import androidx.annotation.StringRes
import com.sf.tadami.R

class PictureInPictureHandler {

    fun update(
        context: Context,
        title: String,
        subtitle: String,
        paused: Boolean,
        replaceWithPrevious: Boolean,
        pipOnExit: Boolean,
        hasNext : Boolean,
        hasPrevious : Boolean,
    ): PictureInPictureParams {

        val pictureInPictureParams = PictureInPictureParams.Builder()
            .setActions(pipActions(context, paused, replaceWithPrevious,hasNext,hasPrevious))
            .setAspectRatio(Rational(16, 9))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pictureInPictureParams.setAutoEnterEnabled(pipOnExit).setSeamlessResizeEnabled(false)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pictureInPictureParams.setTitle(title).setSubtitle(subtitle)
        }

        return pictureInPictureParams.build()
    }

    private fun pipActions(
        context: Context,
        paused: Boolean,
        replaceWithPrevious: Boolean,
        hasNext: Boolean,
        hasPrevious: Boolean
    ): ArrayList<RemoteAction> {
        return arrayListOf(
            if (replaceWithPrevious) {
                createRemoteAction(
                    context,
                    R.drawable.ic_skip_previous_24,
                    R.string.stub_text,
                    PIP_PREVIOUS,
                    PIP_PREVIOUS,
                    hasPrevious,
                )
            } else {
                createRemoteAction(
                    context,
                    R.drawable.ic_forward_10,
                    R.string.stub_text,
                    PIP_SKIP,
                    PIP_SKIP,
                )
            },
            if (paused) {
                createRemoteAction(
                    context,
                    R.drawable.ic_play_24,
                    R.string.stub_text,
                    PIP_PLAY,
                    PIP_PLAY,
                )
            } else {
                createRemoteAction(
                    context,
                    R.drawable.ic_pause_24,
                    R.string.stub_text,
                    PIP_PAUSE,
                    PIP_PAUSE,
                )
            },
            createRemoteAction(
                context,
                R.drawable.ic_skip_next_24,
                R.string.stub_text,
                PIP_NEXT,
                PIP_NEXT,
                hasNext,
            ),
        )
    }

    private fun createRemoteAction(
        context: Context,
        iconResId: Int,
        @StringRes titleRes: Int,
        requestCode: Int,
        controlType: Int,
        isEnabled: Boolean = true,
    ): RemoteAction {
        val action = RemoteAction(
            Icon.createWithResource(context, iconResId),
            context.getString(titleRes),
            context.getString(titleRes),
            PendingIntent.getBroadcast(
                context,
                requestCode,
                Intent(ACTION_MEDIA_CONTROL).putExtra(EXTRA_CONTROL_TYPE, controlType),
                PendingIntent.FLAG_IMMUTABLE,
            ),
        )
        action.isEnabled = isEnabled
        return action
    }
}

// TODO: https://developer.android.com/develop/ui/views/picture-in-picture#setautoenterenabled

internal const val PIP_PLAY = 1
internal const val PIP_PAUSE = 2
internal const val PIP_PREVIOUS = 3
internal const val PIP_NEXT = 4
internal const val PIP_SKIP = 5

internal const val ACTION_MEDIA_CONTROL = "media_control"
internal const val EXTRA_CONTROL_TYPE = "control_type"