package com.binishmatheww.scanner.views.screens

import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.binishmatheww.camera.CameraController
import com.binishmatheww.camera.composables.CameraPreviewLayout
import com.binishmatheww.camera.composables.rememberCameraController
import com.binishmatheww.camera.utils.CameraProp
import com.binishmatheww.camera.utils.SmartSize
import com.binishmatheww.camera.utils.log
import com.binishmatheww.scanner.common.theme.AppTheme
import com.binishmatheww.scanner.views.utils.temporaryLocation
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
fun CameraScreen(
    oncCaptureComplete : (List<File>) -> Unit
) {

    AppTheme.ScannerTheme {

        ConstraintLayout(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {

            val context = LocalContext.current

            val images = remember { mutableStateListOf<File>() }

            val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val notificationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

            val (
                cameraPropsConstraint,
                cameraSizesConstraint,
                cameraPreviewLayoutConstraint,
                flashButtonConstraint,
                filterButtonConstraint,
                captureButtonConstraint,
                nextButtonConstraint,
            ) = createRefs()

            val cameraController = rememberCameraController()

            LaunchedEffect(
                key1 = cameraController,
                block = {
                    cameraController.addRequiredOrientations(listOf(CameraCharacteristics.LENS_FACING_BACK))
                    cameraController.addRequiredFormats(listOf(ImageFormat.JPEG))
                }
            )

            val availableCameraProps by cameraController.availableCameraPropsFlow.collectAsStateWithLifecycle()

            val selectedCameraProp by cameraController.selectedCameraPropFlow.collectAsStateWithLifecycle()

            val availableCameraSizes by cameraController.availableCameraSizesFlow.collectAsStateWithLifecycle()

            val selectedCameraSize by cameraController.selectedCameraSizeFlow.collectAsStateWithLifecycle()

            val isFlashTorchEnabled by cameraController.isFlashTorchEnabledFlow.collectAsStateWithLifecycle()

            val filterKey by remember { mutableStateOf(1.35f) }

            var isFilterEnabled by remember { mutableStateOf(false) }

            var isCaptureButtonEnabled by remember { mutableStateOf(true) }

            CameraPreviewLayout(
                modifier = Modifier.constrainAs(cameraPreviewLayoutConstraint) {
                    linkTo(top = parent.top, bottom = parent.bottom)
                    linkTo(start = parent.start, end = parent.end)
                    width = Dimension.fillToConstraints
                    height = Dimension.fillToConstraints
                },
                cameraController = cameraController
            )

            CameraPropsLayout(
                modifier = Modifier
                    .constrainAs(cameraPropsConstraint){
                        top.linkTo(parent.top, notificationBarHeight.plus(12.dp))
                        linkTo(start = parent.start, end = parent.end)
                    },
                availableCameraProps = availableCameraProps,
                selectedCameraProp = selectedCameraProp,
                onCameraPropSelected = { cameraProp ->
                    cameraController.cameraScope.launch {
                        cameraController.selectCamera(cameraProp)
                        cameraController.selectSize(cameraProp?.outputSizes?.firstOrNull())
                        cameraController.initialize()
                    }
                }
            )

            CameraSizesLayout(
                modifier = Modifier
                    .constrainAs(cameraSizesConstraint){
                        top.linkTo(cameraPropsConstraint.bottom, 12.dp)
                        linkTo(start = parent.start, end = parent.end)
                    },
                availableCameraSizes = availableCameraSizes,
                selectedCameraSize = selectedCameraSize,
                onCameraSizeSelected = { cameraSize ->
                    cameraController.cameraScope.launch {
                        cameraController.selectSize(cameraSize)
                        cameraController.initialize()
                    }
                }
            )

            Button(
                modifier = Modifier
                    .constrainAs(flashButtonConstraint) {
                        linkTo(start = parent.start, end = captureButtonConstraint.start)
                        bottom.linkTo(parent.bottom, navigationBarHeight.plus(12.dp))
                    },
                enabled = isCaptureButtonEnabled,
                onClick = {

                    if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {

                        cameraController.cameraScope.launch {
                            cameraController.toggleFlashTorch()
                        }

                    }
                    else {
                        Toast.makeText(context, "Your phone does not have flashlight support.", Toast.LENGTH_LONG).show()
                    }

                }
            ) {

                Text(
                    text = if(isFlashTorchEnabled) "Flash is on" else "Flash is off"
                )

            }

            Button(
                modifier = Modifier
                    .constrainAs(filterButtonConstraint) {
                        linkTo(start = parent.start, end = captureButtonConstraint.start)
                        bottom.linkTo(flashButtonConstraint.bottom, navigationBarHeight.plus(12.dp))
                    },
                enabled = isCaptureButtonEnabled,
                onClick = {

                    isFilterEnabled = !isFilterEnabled

                }
            ) {

                Text(
                    text = if(isFilterEnabled) "Filter is on" else "Filter is off"
                )

            }

            Button(
                modifier = Modifier
                    .constrainAs(captureButtonConstraint) {
                        linkTo(start = parent.start, end = parent.end)
                        bottom.linkTo(parent.bottom, navigationBarHeight.plus(12.dp))
                    },
                enabled = isCaptureButtonEnabled,
                onClick = {

                    cameraController.cameraScope.launch {

                        isCaptureButtonEnabled = false
                        cameraController.captureImage{ cameraCharacteristics, combinedCaptureResult ->

                            val file = cameraController.saveCapturedImageAsFile(
                                characteristics = cameraCharacteristics,
                                result = combinedCaptureResult,
                                fileLocation = temporaryLocation(context = context)
                            )

                            if(file.exists()){

                                if (file.extension == "jpg") {
                                    ExifInterface(file.absolutePath).apply {
                                        setAttribute(ExifInterface.TAG_ORIENTATION, combinedCaptureResult.orientation.toString())
                                        saveAttributes()
                                    }
                                }

                                images.add(file)

                            }

                        }
                        isCaptureButtonEnabled = true

                    }

                }
            ) {

                Text(
                    text = "Capture"
                )

            }

            Button(
                modifier = Modifier
                    .constrainAs(nextButtonConstraint) {
                        linkTo(start = captureButtonConstraint.end, end = parent.end)
                        bottom.linkTo(parent.bottom, navigationBarHeight.plus(12.dp))
                    },
                enabled = isCaptureButtonEnabled,
                onClick = {

                    oncCaptureComplete.invoke(images)

                }
            ) {

                Text(
                    text = "Next"
                )

            }


        }

    }

}

@Composable
fun CameraPropsLayout(
    modifier: Modifier,
    availableCameraProps : List<CameraProp>,
    selectedCameraProp : CameraProp?,
    onCameraPropSelected : (CameraProp?) -> Unit
){

    LazyRow(
        modifier = modifier,
    ){

        items(
            items = availableCameraProps,
        ){ cameraProp ->

            Button(
                modifier = Modifier
                    .height(32.dp)
                    .padding(
                        horizontal = 8.dp
                    ),
                onClick = {
                    onCameraPropSelected.invoke(cameraProp)
                },
                colors = ButtonDefaults.buttonColors(
                    contentColor = Color.Black,
                    backgroundColor = if(cameraProp == selectedCameraProp) Color.Green else Color.Gray
                )
            ) {
                Text(text = cameraProp.formatName)
            }
        }

    }

}

@Composable
fun CameraSizesLayout(
    modifier: Modifier,
    availableCameraSizes: List<SmartSize>,
    selectedCameraSize: SmartSize?,
    onCameraSizeSelected : (SmartSize) -> Unit
){

    LazyRow(
        modifier = modifier,
    ){

        items(
            items = availableCameraSizes,
        ){ cameraSize ->

            Button(
                modifier = Modifier
                    .height(32.dp)
                    .padding(
                        horizontal = 8.dp
                    ),
                onClick = {
                    onCameraSizeSelected.invoke(cameraSize)
                },
                colors = ButtonDefaults.buttonColors(
                    contentColor = Color.Black,
                    backgroundColor = if(cameraSize == selectedCameraSize) Color.Green else Color.Gray
                )
            ) {
                Text(text = "${cameraSize.size.width} x ${cameraSize.size.height}")
            }

        }

    }

}
