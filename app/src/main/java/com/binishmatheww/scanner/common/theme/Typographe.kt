package com.binishmatheww.scanner.common.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.binishmatheww.scanner.R

object Typographe {

    private val raleway = FontFamily(
        Font(R.font.raleway_light, FontWeight.Light),
        Font(R.font.raleway_medium, FontWeight.Medium),
        Font(R.font.raleway_bold, FontWeight.Bold)
    )

    private val primaryFont = raleway

    private val displayLarge = TextStyle(
        fontFamily = primaryFont,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp
    )

    private val displayMedium = TextStyle(
        fontFamily = primaryFont,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp
    )

    private val displaySmall = TextStyle(
        fontFamily = primaryFont,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp
    )

    private val headlineLarge = TextStyle(
        fontFamily = primaryFont,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp
    )

    private val headlineMedium = TextStyle(
        fontFamily = primaryFont,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp
    )

    private val headlineSmall = TextStyle(
        fontFamily = primaryFont,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp
    )

    private val titleLarge = TextStyle(
        fontFamily = primaryFont,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp
    )

    private val titleMedium = TextStyle(
        fontFamily = primaryFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    )

    private val titleSmall = TextStyle(
        fontFamily = primaryFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    )

    private val bodyLarge = TextStyle(
        fontFamily = primaryFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    )

    private val bodyMedium = TextStyle(
        fontFamily = primaryFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    )

    private val bodySmall = TextStyle(
        fontFamily = primaryFont,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    )

    private val labelLarge = TextStyle(
        fontFamily = primaryFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    )

    private val labelMedium = TextStyle(
        fontFamily = primaryFont,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    )

    private val labelSmall = TextStyle(
        fontFamily = primaryFont,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp
    )

    private val bold14 = TextStyle(
        fontFamily = raleway,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
    )



    val typography = Typography(
        displayLarge = displayLarge,
        displayMedium = displayMedium,
        displaySmall = displaySmall,
        headlineLarge = headlineLarge,
        headlineMedium = headlineMedium,
        headlineSmall = headlineSmall,
        titleLarge = titleLarge,
        titleMedium = titleMedium,
        titleSmall = titleSmall,
        bodyLarge = bodyLarge,
        bodyMedium = bodyMedium,
        bodySmall = bodySmall,
        labelLarge = labelLarge,
        labelMedium = labelMedium,
        labelSmall = labelSmall,
    )

}