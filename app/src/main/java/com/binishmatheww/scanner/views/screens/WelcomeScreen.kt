package com.binishmatheww.scanner.views.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.binishmatheww.scanner.R
import com.binishmatheww.scanner.common.theme.AppTheme
import kotlinx.coroutines.delay

@Composable
fun WelcomeScreen(
    onComplete: () -> Unit
) {

    AppTheme.ScannerTheme {

        LaunchedEffect(
            key1 = true,
            block = {
                delay(2000)
                onComplete.invoke()
            }
        )

        ConstraintLayout(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {

            val (
                appIconConstraint
            ) = createRefs()

            Image(
                modifier = Modifier
                    .constrainAs(appIconConstraint) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
                    .size(200.dp),
                painter = painterResource(id = R.drawable.cameraic),
                contentDescription = LocalContext.current.getString(R.string.app_name),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
            )

        }

    }

}