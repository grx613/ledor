package org.ledor.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ledor.R
import org.ledor.ui.theme.KalendarColors
import org.ledor.ui.theme.Rubik

/**
 * Логотип и слоган (BACKLOG B-13): знак + «LeDor» — один блок; слоган — отдельный блок
 * после заметного отступа, мельче и легче.
 */
@Composable
fun LogoBlock(onDark: Boolean, markSize: Dp = 96.dp, modifier: Modifier = Modifier) {
    val main = if (onDark) Color.White else MaterialTheme.colorScheme.onBackground
    val soft = if (onDark) Color.White.copy(alpha = 0.72f) else MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        LedorMark(size = markSize)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.app_name),
            style = TextStyle(fontFamily = Rubik, fontWeight = FontWeight.SemiBold, fontSize = 40.sp, letterSpacing = 0.5.sp, color = main))
        Spacer(Modifier.height(28.dp))
        Text(stringResource(R.string.slogan_latin),
            style = TextStyle(fontFamily = Rubik, fontWeight = FontWeight.Normal, fontSize = 16.sp, letterSpacing = 1.5.sp, color = soft))
        Spacer(Modifier.height(2.dp))
        Text(stringResource(R.string.slogan_hebrew),
            style = TextStyle(fontFamily = Rubik, fontWeight = FontWeight.Normal, fontSize = 17.sp, color = soft))
    }
}

/** Изящная надпись «Kalendář» внизу экрана: средний размер, разрядка, золото. */
@Composable
fun Wordmark(onDark: Boolean, modifier: Modifier = Modifier) {
    val gold = if (onDark) Color(0xFFF2C75C) else KalendarColors.extra.gold
    Text(
        stringResource(R.string.wordmark),
        modifier = modifier,
        style = TextStyle(fontFamily = Rubik, fontWeight = FontWeight.Medium, fontSize = 21.sp, letterSpacing = 4.sp, color = gold),
    )
}
