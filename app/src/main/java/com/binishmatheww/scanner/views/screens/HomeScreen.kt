package com.binishmatheww.scanner.views.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.DrawerValue
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.ModalDrawer
import androidx.compose.material.rememberDrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.content.ContextCompat.checkSelfPermission
import com.binishmatheww.scanner.R
import com.binishmatheww.scanner.common.theme.AppTheme

@Composable
fun HomeScreen(
    onCameraClick : () -> Unit
) {

    AppTheme.ScannerTheme {

        val context = LocalContext.current

        val drawerState = rememberDrawerState(DrawerValue.Closed)

        val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        ModalDrawer(
            drawerState = drawerState,
            drawerContent = {

                ConstraintLayout(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {




                }

            }
        ) {

            ConstraintLayout(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {

                var hasCameraPermission by remember { mutableStateOf(checkSelfPermission(context,Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }

                val cameraPermissionRequestLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted: Boolean ->
                    hasCameraPermission = isGranted
                }

                val (
                editorActionButtonConstraint,
                cameraActionButtonConstraint,
                ) = createRefs()



                FloatingActionButton(
                    modifier = Modifier
                        .constrainAs(editorActionButtonConstraint) {
                            start.linkTo(cameraActionButtonConstraint.start)
                            end.linkTo(cameraActionButtonConstraint.end)
                            bottom.linkTo(cameraActionButtonConstraint.top, 12.dp)
                        }
                        .size(66.dp),
                    backgroundColor = MaterialTheme.colorScheme.primary,
                    onClick = {

                    },
                    content = {
                        Icon(
                            modifier = Modifier
                                .size(48.dp),
                            painter = painterResource(id = R.drawable.document_icon),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            contentDescription = "",
                        )
                    }
                )

                FloatingActionButton(
                    modifier = Modifier
                        .constrainAs(cameraActionButtonConstraint) {
                            end.linkTo(parent.end, 24.dp)
                            bottom.linkTo(parent.bottom, navigationBarHeight.plus(24.dp))
                        }
                        .size(66.dp),
                    backgroundColor = MaterialTheme.colorScheme.primary,
                    onClick = {

                        if(hasCameraPermission){
                            onCameraClick.invoke()
                        }
                        else{
                            cameraPermissionRequestLauncher.launch(Manifest.permission.CAMERA)
                        }

                    },
                    content = {
                        Icon(
                            modifier = Modifier
                                .size(48.dp),
                            painter = painterResource(id = R.drawable.cameraic),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            contentDescription = "",
                        )
                    }
                )


            }

        }

    }

}