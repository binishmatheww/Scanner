package com.binishmatheww.scanner.common.theme

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp
import com.google.accompanist.systemuicontroller.rememberSystemUiController

object AppTheme {

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun ScannerTheme(
        darkTheme: Boolean = isSystemInDarkTheme(),
        content: @Composable () -> Unit
    ) {
        val colors = if (darkTheme) {
            ColorPalette.darkColorScheme
        } else {
            ColorPalette.lightColorScheme
        }

        val systemUiController = rememberSystemUiController()

        systemUiController.setSystemBarsColor(
            color = colors.background,
            darkIcons = !darkTheme
        )


        MaterialTheme(
            colorScheme = colors,
            typography = Typographe.typography,
            shapes = shapes,
        ) {
            CompositionLocalProvider(
                LocalOverscrollConfiguration provides null,
                content = content
            )
        }
    }

    private val shapes = Shapes(
        small = RoundedCornerShape(2.dp),
        medium = RoundedCornerShape(4.dp),
        large = RoundedCornerShape(8.dp)
    )

}
