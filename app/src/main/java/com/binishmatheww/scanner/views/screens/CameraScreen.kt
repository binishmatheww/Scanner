package com.binishmatheww.scanner.views.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.core.content.ContextCompat
import com.binishmatheww.camera.composables.CameraPreviewLayout
import com.binishmatheww.camera.composables.rememberCameraController
import com.binishmatheww.scanner.common.theme.AppTheme

@Composable
fun CameraScreen() {

    AppTheme.ScannerTheme {

        ConstraintLayout(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {

            val context = LocalContext.current

            val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

            val (cameraPreviewConstraint, shutterButtonConstraint) = createRefs()

            val cameraController = rememberCameraController()

            CameraPreviewLayout(
                modifier = Modifier.constrainAs(cameraPreviewConstraint) {
                    linkTo(top = parent.top, bottom = parent.bottom)
                    linkTo(start = parent.start, end = parent.end)
                    width = Dimension.fillToConstraints
                    height = Dimension.fillToConstraints
                },
                cameraController = cameraController
            )

            Button(modifier = Modifier.constrainAs(shutterButtonConstraint) {
                linkTo(start = parent.start, end = parent.end)
                bottom.linkTo(parent.bottom, navigationBarHeight.plus(16.dp))
                width = Dimension.preferredWrapContent
                height = Dimension.preferredWrapContent
            }, onClick = {

            })
            {
                Text("Hello")
            }


        }

    }

}
