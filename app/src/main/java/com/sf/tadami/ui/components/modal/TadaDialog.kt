package com.sf.tadami.ui.components.modal

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sf.tadami.R

/** A button of a dialog. [busy] shows a progress in its place, it can't be clicked then. */
data class ModalAction(
    val label: String,
    val enabled: Boolean = true,
    val busy: Boolean = false,
    val onClick: () -> Unit,
)

enum class DialogTone { Default, Destructive }

/** What a dialog is about, e.g. an extension: its icon, name and details, in the header. */
class ModalSubject(
    val name: String,
    val meta: String?,
    val icon: @Composable (Modifier) -> Unit,
)

/** A checkbox of a dialog, e.g. "Also reset all the episodes". */
data class DialogOption(val label: String, val checked: Boolean, val onCheckedChange: (Boolean) -> Unit)

private val DialogPadding = 20.dp
private val BlockSpacing = 16.dp
private const val ScrimAlpha = 0.4f

/**
 * The dialog every centred modal is drawn with: a header (icon badge or subject, title, subtitle) and
 * the buttons stay put, the body scrolls between them. Width: the window minus 24dp margins, between
 * 280 and 560dp. Text buttons at the end, [neutral] at the start, the confirm one in the error colour
 * when [tone] is destructive.
 *
 * @param padBody false when the body is a list whose rows span the whole width
 * @param scrollable false when the body is a lazy list, which scrolls by itself
 */
@Composable
fun TadaDialog(
    onDismissRequest: () -> Unit,
    title: String?,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    subtitle: String? = null,
    subject: ModalSubject? = null,
    confirm: ModalAction? = null,
    dismiss: ModalAction? = ModalAction(stringResource(R.string.action_cancel), onClick = onDismissRequest),
    neutral: ModalAction? = null,
    tone: DialogTone = DialogTone.Default,
    dismissOnClickOutside: Boolean = true,
    dismissOnBackPress: Boolean = true,
    padBody: Boolean = true,
    scrollable: Boolean = true,
    headerAction: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalWindow(
        onDismissRequest = onDismissRequest,
        scrimAlpha = ScrimAlpha,
        dismissOnBackPress = dismissOnBackPress,
        dismissOnClickOutside = dismissOnClickOutside,
        modifier = modifier
            .widthIn(min = ModalDefaults.MinWidth, max = ModalDefaults.MaxWidth)
            .fillMaxWidth(),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            // surfaceContainer is the 0x0C0C0C band on pure black
            color = if (isPureBlack()) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer,
            border = modalBorder(always = true),
        ) {
            DialogFrame(
                title = title,
                icon = icon,
                subtitle = subtitle,
                subject = subject,
                confirm = confirm,
                dismiss = dismiss,
                neutral = neutral,
                tone = tone,
                padBody = padBody,
                scrollable = scrollable,
                headerAction = headerAction,
                content = content,
            )
        }
    }
}

@Composable
private fun DialogFrame(
    title: String?,
    icon: ImageVector?,
    subtitle: String?,
    subject: ModalSubject?,
    confirm: ModalAction?,
    dismiss: ModalAction?,
    neutral: ModalAction?,
    tone: DialogTone,
    padBody: Boolean,
    scrollable: Boolean,
    headerAction: (@Composable () -> Unit)?,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.padding(top = DialogPadding)) {
        if (title != null || icon != null || subject != null) {
            DialogHeader(
                title = title,
                icon = icon,
                subtitle = subtitle,
                subject = subject,
                tone = tone,
                headerAction = headerAction,
                modifier = Modifier.padding(horizontal = DialogPadding),
            )
            Spacer(Modifier.height(BlockSpacing))
        }
        val bodyPadding = Modifier.padding(horizontal = if (padBody) DialogPadding else 0.dp)
        // The body's text: bodyMedium, onSurfaceVariant
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant,
            LocalTextStyle provides MaterialTheme.typography.bodyMedium,
        ) {
            if (scrollable) {
                val scroll = rememberScrollState()
                Box(modifier = Modifier.weight(1f, fill = false)) {
                    Column(modifier = Modifier.verticalScroll(scroll).then(bodyPadding), content = content)
                    ScrollFades(scroll, dialogContainer())
                }
            } else {
                Column(modifier = Modifier.weight(1f, fill = false).then(bodyPadding), content = content)
            }
        }
        if (confirm != null || dismiss != null || neutral != null) {
            Spacer(Modifier.height(BlockSpacing))
            DialogButtons(
                confirm = confirm,
                dismiss = dismiss,
                neutral = neutral,
                tone = tone,
                modifier = Modifier.padding(horizontal = DialogPadding - 8.dp),
            )
            Spacer(Modifier.height(DialogPadding - 4.dp))
        } else {
            Spacer(Modifier.height(DialogPadding))
        }
    }
}

@Composable
private fun dialogContainer(): Color =
    if (isPureBlack()) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer

/** A 16dp fade at the edge the body can still scroll to. */
@Composable
private fun BoxScope.ScrollFades(scroll: ScrollState, color: Color) {
    Column(modifier = Modifier.matchParentSize()) {
        if (scroll.canScrollBackward) {
            Box(Modifier.fillMaxWidth().height(16.dp).background(Brush.verticalGradient(listOf(color, Color.Transparent))))
        }
        Spacer(Modifier.weight(1f))
        if (scroll.canScrollForward) {
            Box(Modifier.fillMaxWidth().height(16.dp).background(Brush.verticalGradient(listOf(Color.Transparent, color))))
        }
    }
}

/** A 40dp round badge with [icon], in the error colours for a destructive dialog. */
@Composable
private fun IconBadge(icon: ImageVector, tone: DialogTone) {
    val colors = MaterialTheme.colorScheme
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (tone == DialogTone.Destructive) colors.errorContainer else colors.secondaryContainer),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (tone == DialogTone.Destructive) colors.onErrorContainer else colors.onSecondaryContainer,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** The badge (or the subject's icon) leading the title, its subtitle or the subject's name and details under it. */
@Composable
private fun DialogHeader(
    title: String?,
    icon: ImageVector?,
    subtitle: String?,
    subject: ModalSubject?,
    tone: DialogTone,
    headerAction: (@Composable () -> Unit)?,
    modifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        when {
            subject != null -> subject.icon(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)))
            icon != null -> IconBadge(icon, tone)
        }
        if (subject != null || icon != null) Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            title?.let { Text(it, style = MaterialTheme.typography.titleLarge) }
            if (subject != null) {
                Text(subject.name, style = MaterialTheme.typography.bodyMedium)
                subject.meta?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant) }
            }
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant) }
        }
        headerAction?.invoke()
    }
}

/** A paragraph of a dialog's body. */
@Composable
fun ModalBodyText(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = color,
        modifier = modifier.fillMaxWidth(),
    )
}

/** A warning under a dialog's message, in the error colour. */
@Composable
fun ModalConsequence(text: String, modifier: Modifier = Modifier) {
    val error = MaterialTheme.colorScheme.error
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = error, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = error)
    }
}

@Composable
private fun ButtonLabel(action: ModalAction) {
    if (action.busy) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = LocalContentColor.current,
        )
    } else {
        Text(action.label)
    }
}

@Composable
private fun DialogButtons(
    confirm: ModalAction?,
    dismiss: ModalAction?,
    neutral: ModalAction?,
    tone: DialogTone,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        neutral?.let { TextButton(onClick = it.onClick, enabled = it.enabled) { Text(it.label) } }
        Spacer(Modifier.weight(1f))
        dismiss?.let { TextButton(onClick = it.onClick, enabled = it.enabled) { Text(it.label) } }
        if (dismiss != null && confirm != null) Spacer(Modifier.width(8.dp))
        confirm?.let {
            TextButton(
                onClick = it.onClick,
                enabled = it.enabled && !it.busy,
                colors = if (tone == DialogTone.Destructive) {
                    ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                } else {
                    ButtonDefaults.textButtonColors()
                },
            ) { ButtonLabel(it) }
        }
    }
}

/**
 * A destructive confirmation: a delete badge (or the subject's icon), the confirm button in the error
 * colour. [onConfirm] is the button's click, it doesn't close the dialog by itself.
 */
@Composable
fun TadaDestructiveDialog(
    title: String?,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = Icons.Outlined.Delete,
    subtitle: String? = null,
    subject: ModalSubject? = null,
    consequence: String? = null,
    option: DialogOption? = null,
) {
    TadaDialog(
        onDismissRequest = onDismissRequest,
        title = title,
        modifier = modifier,
        icon = icon,
        subtitle = subtitle,
        subject = subject,
        tone = DialogTone.Destructive,
        confirm = ModalAction(confirmLabel, onClick = onConfirm),
    ) {
        ModalBodyText(message)
        consequence?.let {
            Spacer(Modifier.height(12.dp))
            ModalConsequence(it)
        }
        option?.let {
            Spacer(Modifier.height(8.dp))
            ModalCheckboxRow(it.label, it.checked, it.onCheckedChange)
        }
    }
}
