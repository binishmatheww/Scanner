package com.binishmatheww.scanner.views.composables

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter

@Composable
fun ClickableIcon(
    modifier: Modifier,
    painter: Painter,
    contentDescription: String? = null,
    onClick: () -> Unit
) {

    IconButton(
        modifier = modifier,
        onClick = onClick
    ) {

        Icon(
            painter = painter,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary
        )

    }

}