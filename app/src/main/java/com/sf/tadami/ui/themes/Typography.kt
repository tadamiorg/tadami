package com.sf.tadami.ui.themes


import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import org.jetbrains.compose.resources.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sf.tadami.shared.resources.Res
import com.sf.tadami.shared.resources.*

/** Roboto, the app's font, read from the shared resources. */
@Composable
fun fonts() = FontFamily(
    Font(Res.font.roboto, weight = FontWeight.Normal),
    Font(Res.font.roboto_medium, weight = FontWeight.Medium),
    Font(Res.font.roboto_bold, weight = FontWeight.Bold),
    Font(Res.font.roboto_extrabold, weight = FontWeight.ExtraBold),
)

@Composable
fun tadamiTypography(): Typography {
    val fonts = fonts()
    return Typography(
    labelMedium = TextStyle(fontFamily = fonts, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 12.5.sp),
    labelSmall = TextStyle(fontFamily = fonts, fontWeight = FontWeight.Normal, fontSize = 13.sp,),
    headlineSmall = TextStyle(fontFamily = fonts, fontWeight = FontWeight.Medium, fontSize = 18.sp)
)
}

val Typography.header: TextStyle
    @Composable
    get() = bodyMedium.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
    )
