package org.ledor.ui.splash

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.ledor.R
import org.ledor.ui.components.LedorMark
import org.ledor.ui.components.LogoBlock
import org.ledor.ui.components.Wordmark
import org.ledor.ui.components.drawMagenDavid

/**
 * Заставка (BACKLOG B-9, B-13): знак + LeDor; с отступом — слоган на латинице и иврите;
 * внизу экрана — изящная надпись Kalendář.
 */
@Composable
fun BrandSplash() {
    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0B3FAE), Color(0xFF061F5C))))
            .clickable(remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val step = 72.dp.toPx()
            val c = Color.White.copy(alpha = 0.05f)
            var row = 0
            var y = 0f
            while (y < size.height + step) {
                var x = if (row % 2 == 0) 0f else step / 2
                while (x < size.width + step) {
                    drawMagenDavid(c, 1.dp.toPx(), center = Offset(x, y), sizePx = 28.dp.toPx())
                    x += step
                }
                y += step * 0.87f
                row++
            }
        }
        LogoBlock(onDark = true, markSize = 104.dp)
        Wordmark(
            onDark = true,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 40.dp),
        )
    }
}
