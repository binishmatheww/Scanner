package com.binishmatheww.scanner.views.fragments

import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.findNavController
import com.binishmatheww.camera.composables.CameraPreviewLayout
import com.binishmatheww.camera.composables.rememberCameraController
import com.binishmatheww.camera.utils.SmartSize
import com.binishmatheww.scanner.R
import com.binishmatheww.scanner.common.theme.AppTheme
import com.binishmatheww.scanner.common.utils.animateScrollAndCentralizeItem
import com.binishmatheww.scanner.common.utils.temporaryLocation
import kotlinx.coroutines.launch
import java.io.File
import java.io.Serializable


class CameraFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

        return ComposeView(context = layoutInflater.context).apply {
            setContent {
                CameraScreen{ images ->

                    if (images.isEmpty()) {
                        Toast.makeText(activity ?: return@CameraScreen, "No images taken", Toast.LENGTH_SHORT).show()
                    }
                    else {

                        activity?.findNavController(R.id.navigationController)
                            ?.navigate(
                                R.id.action_cameraFragment_to_pdfEditorFragment,
                                Bundle().apply {
                                    putBundle(
                                        "images",
                                        Bundle().apply {
                                            putSerializable(
                                                "pages",
                                                ArrayList(images.toList()) as Serializable
                                            )
                                        }
                                    )
                                }
                            )

                    }

                }
            }
        }

    }

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
                    cameraSizesConstraint,
                    cameraPreviewLayoutConstraint,
                    flashButtonConstraint,
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

                val availableCameraSizes by cameraController.availableCameraSizesFlow.collectAsStateWithLifecycle()

                val selectedCameraSize by cameraController.selectedCameraSizeFlow.collectAsStateWithLifecycle()

                val isFlashTorchEnabled by cameraController.isFlashTorchEnabledFlow.collectAsStateWithLifecycle()

                //val filterKey by remember { mutableStateOf(1.35f) }

                //var isFilterEnabled by remember { mutableStateOf(false) }

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

                CameraSizesLayout(
                    modifier = Modifier
                        .constrainAs(cameraSizesConstraint){
                            bottom.linkTo(captureButtonConstraint.top, 12.dp)
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

                Image(
                    modifier = Modifier
                        .constrainAs(flashButtonConstraint) {
                            top.linkTo(parent.top, notificationBarHeight.plus(4.dp))
                            end.linkTo(parent.end, 4.dp)
                        }
                        .size(48.dp)
                        .clickable {

                            if (!isCaptureButtonEnabled) return@clickable

                            if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {

                                cameraController.cameraScope.launch {
                                    cameraController.toggleFlashTorch()
                                }

                            } else {
                                Toast
                                    .makeText(
                                        context,
                                        "Your phone does not have a flashlight",
                                        Toast.LENGTH_LONG
                                    )
                                    .show()
                            }

                        },
                    painter = painterResource(id = R.drawable.flash_button),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(
                        if(isFlashTorchEnabled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.secondary
                    )
                )

                Image(
                    modifier = Modifier
                        .constrainAs(captureButtonConstraint) {
                            linkTo(start = parent.start, end = parent.end)
                            bottom.linkTo(parent.bottom, navigationBarHeight.plus(12.dp))
                        }
                        .size(60.dp)
                        .clickable {

                            if (!isCaptureButtonEnabled) return@clickable

                            cameraController.cameraScope.launch {

                                isCaptureButtonEnabled = false
                                cameraController.captureImage { cameraCharacteristics, combinedCaptureResult ->

                                    val file = cameraController.saveCapturedImageAsFile(
                                        characteristics = cameraCharacteristics,
                                        result = combinedCaptureResult,
                                        fileLocation = temporaryLocation(context = context)
                                    )

                                    if (file.exists()) {

                                        if (file.extension == "jpg") {
                                            ExifInterface(file.absolutePath).apply {
                                                setAttribute(
                                                    ExifInterface.TAG_ORIENTATION,
                                                    combinedCaptureResult.orientation.toString()
                                                )
                                                saveAttributes()
                                            }
                                        }

                                        images.add(file)

                                    }

                                }
                                isCaptureButtonEnabled = true

                            }

                        },
                    painter = painterResource(id = R.drawable.camera_button),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(
                        if(isCaptureButtonEnabled) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.primary
                    )
                )

                Image(
                    modifier = Modifier
                        .constrainAs(nextButtonConstraint) {
                            linkTo(start = captureButtonConstraint.end, end = parent.end)
                            bottom.linkTo(parent.bottom, navigationBarHeight.plus(12.dp))
                        }
                        .size(60.dp)
                        .clickable {
                            oncCaptureComplete.invoke(images)
                            if (isFlashTorchEnabled) {
                                cameraController.cameraScope.launch {
                                    cameraController.toggleFlashTorch()
                                }
                            }
                        },
                    painter = painterResource(id = R.drawable.nextic),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(
                        if(images.isEmpty()) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.primary
                    )
                )


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

        val cameraSizeLayoutState = rememberLazyListState()

        LazyRow(
            modifier = modifier,
            state = cameraSizeLayoutState
        ){

            items(
                items = availableCameraSizes,
            ){ cameraSize ->

                Text(
                    modifier = Modifier
                        .height(32.dp)
                        .padding(
                            horizontal = 8.dp
                        )
                        .clickable {
                            onCameraSizeSelected.invoke(cameraSize)
                        },
                    text = "${cameraSize.size.width} x ${cameraSize.size.height}",
                    color = if(cameraSize == selectedCameraSize) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.secondary
                )

            }

        }

        LaunchedEffect(
            key1 = selectedCameraSize,
            block = {

                if( availableCameraSizes.contains(selectedCameraSize) ){

                    cameraSizeLayoutState.animateScrollAndCentralizeItem(
                        availableCameraSizes.indexOf(
                            selectedCameraSize),
                        this
                    )

                }

            }
        )

    }

}