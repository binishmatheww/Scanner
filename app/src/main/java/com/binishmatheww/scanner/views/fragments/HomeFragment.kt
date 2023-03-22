package com.binishmatheww.scanner.views.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.DrawerValue
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.ModalDrawer
import androidx.compose.material.rememberDrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import com.binishmatheww.scanner.R
import com.binishmatheww.scanner.common.theme.AppTheme
import com.binishmatheww.scanner.common.utils.getPdfFiles
import com.binishmatheww.scanner.common.utils.hasExternalStoragePermissions
import com.binishmatheww.scanner.common.utils.requestExternalStoragePermissions
import com.binishmatheww.scanner.models.PdfFile
import com.binishmatheww.scanner.viewmodels.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private val homeViewModel by viewModels<HomeViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

        return ComposeView(context = layoutInflater.context).apply {
            setContent {
                HomeScreen(
                    pdfFiles = homeViewModel.files,
                    onCameraClick =  {

                        activity
                            ?.findNavController(R.id.navigationController)
                            ?.navigate(R.id.action_homeFragment_to_cameraFragment)

                    },
                    onEditorClick = {

                        activity
                            ?.findNavController(R.id.navigationController)
                            ?.navigate(R.id.action_homeFragment_to_pdfEditorFragment,)

                    },
                    onFileClick = { pdfFile ->

                        activity
                            ?.findNavController(R.id.navigationController)
                            ?.navigate(
                                R.id.action_homeFragment_to_pdfEditorFragment,
                                Bundle().apply {
                                    putString(
                                        "uri",
                                        pdfFile.uri?.toString()
                                    )
                                }
                            )

                    }
                )
            }
        }

    }
    override fun onResume() {
        super.onResume()

        activity?.apply {

            if(hasExternalStoragePermissions()){
                homeViewModel.files.clear()
                homeViewModel.files.addAll(getPdfFiles())
            }
            else{
                requestExternalStoragePermissions()
            }

        }

    }

    @Composable
    fun HomeScreen(
        pdfFiles: SnapshotStateList<PdfFile>,
        onCameraClick: () -> Unit,
        onEditorClick: () -> Unit,
        onFileClick: (PdfFile) -> Unit
    ) {

        AppTheme.ScannerTheme {

            val context = LocalContext.current

            val drawerState = rememberDrawerState(DrawerValue.Closed)

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
                        .padding(4.dp)
                ) {

                    var hasCameraPermission by remember { mutableStateOf(checkSelfPermission(context,Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }

                    val cameraPermissionRequestLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { isGranted: Boolean ->
                        hasCameraPermission = isGranted
                    }

                    val (
                        pdfFilePreviewConstraint,
                        editorActionButtonConstraint,
                        cameraActionButtonConstraint,
                    ) = createRefs()

                    LazyColumn(
                        modifier = Modifier
                            .constrainAs(pdfFilePreviewConstraint) {
                                top.linkTo(parent.top)
                                bottom.linkTo(parent.bottom)
                                start.linkTo(parent.start)
                                end.linkTo(parent.end)
                            }
                            .fillMaxSize(),
                        content = {
                            
                            itemsIndexed(pdfFiles){ index, pdfFile ->
                                
                                PdfFilePreview(
                                    modifier = Modifier
                                        .fillMaxSize(),
                                    pdfFile = pdfFile,
                                    onFileClick = onFileClick
                                )
                                
                            }
                            
                        }
                    )

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
                                  onEditorClick.invoke()
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
                                end.linkTo(parent.end, 12.dp)
                                bottom.linkTo(parent.bottom, 24.dp)
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
    
    @Composable
    private fun PdfFilePreview(
        modifier: Modifier = Modifier,
        pdfFile: PdfFile,
        onFileClick: (PdfFile) -> Unit
    ){
        
        Row(
            modifier = modifier
                .clickable {
                    onFileClick.invoke(pdfFile)
                }
        ) {
            
            Text(
                modifier = Modifier
                    .fillMaxSize(),
                color = MaterialTheme.colorScheme.primary,
                text = pdfFile.name ?: "Unknown"
            )
            
        }
        
    }

}