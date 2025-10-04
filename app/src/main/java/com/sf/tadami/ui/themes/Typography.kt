package com.sf.tadami.ui.themes


import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sf.tadami.R

val Fonts = FontFamily(
    Font(R.font.roboto, weight = FontWeight.Normal),
    Font(R.font.roboto_medium, weight = FontWeight.Medium),
    Font(R.font.roboto_bold, weight = FontWeight.Bold),
    Font(R.font.roboto_extrabold, weight = FontWeight.ExtraBold),
)

val Typography = Typography(
    labelMedium = TextStyle(fontFamily = Fonts, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 12.5.sp),
    labelSmall = TextStyle(fontFamily = Fonts, fontWeight = FontWeight.Normal, fontSize = 13.sp,),
    headlineSmall = TextStyle(fontFamily = Fonts, fontWeight = FontWeight.Medium, fontSize = 18.sp)
)

val Typography.header: TextStyle
    @Composable
    get() = bodyMedium.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
    )
