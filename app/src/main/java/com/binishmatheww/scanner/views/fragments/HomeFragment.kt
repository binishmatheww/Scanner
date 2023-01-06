package com.binishmatheww.scanner.views.fragments

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.compose.ui.platform.ComposeView
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.binishmatheww.scanner.R
import com.binishmatheww.scanner.views.adapters.FilePickerAdapter
import com.binishmatheww.scanner.common.utils.openEditor
import com.binishmatheww.scanner.common.utils.pdfFilesFromStorageLocation
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import java.io.File
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
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
import com.binishmatheww.scanner.common.theme.AppTheme


class HomeFragment : Fragment() {

    private lateinit var cameraActionButton: FloatingActionButton
    private lateinit var editorActionButton: FloatingActionButton
    private lateinit var homeRecyclerView: RecyclerView
    private lateinit var homeLinearLayout: LinearLayout
    private lateinit var filePickerAdapter: FilePickerAdapter

    private var files: ArrayList<File> = ArrayList()

    private lateinit var toggle : ActionBarDrawerToggle

    var drawerLayout : DrawerLayout? = null
    var drawer : NavigationView? = null

    private var outFile : File? = null

    private lateinit var filePickerLauncher : ActivityResultLauncher<Intent>

    private lateinit var documentCreatorLauncher : ActivityResultLauncher<Intent>

    private lateinit var cameraPermissionLauncher : ActivityResultLauncher<String>


    override fun onAttach(context: Context) {
        super.onAttach(context)
        filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
            if(result.resultCode == RESULT_OK){
                result?.data?.data?.let { uri ->
                    openEditor(uri)
                }
            }
        }
        documentCreatorLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result?.data?.data?.let { uri ->
                    requireActivity().contentResolver.openOutputStream(uri)?.let{ os ->
                        Log.wtf("uri",uri.toString())
                        outFile?.let {
                            os.write(it.readBytes())
                            os.close()
                            outFile = null
                        }
                    }
                }
            }
        }
        cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                requireActivity().supportFragmentManager.beginTransaction().replace(R.id.navigationController, CameraFragment(), "camera").addToBackStack("camera").commitAllowingStateLoss()
            } else {
                Toast.makeText(requireContext(),"Please enable camera permission",Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

        return ComposeView(context = layoutInflater.context).apply {
            setContent {
                HomeScreen(
                    onCameraClick =  {
                        activity?.findNavController(R.id.navigationController)?.navigate(R.id.action_homeFragment_to_cameraFragment)
                    },
                    onEditorClick = {
                        activity?.findNavController(R.id.navigationController)?.navigate(R.id.action_homeFragment_to_pdfEditorFragment)
                    }
                )
            }
        }

    }

    override fun onViewCreated(layout: View, savedInstanceState: Bundle?) {
        super.onViewCreated(layout, savedInstanceState)

        /*

        cameraActionButton.setOnClickListener {
            when (PERMISSION_GRANTED) {
                checkSelfPermission(requireContext(),CAMERA) -> {
                    requireActivity().supportFragmentManager.beginTransaction().replace(R.id.fContainer, CameraFragment(), "camera").addToBackStack("camera").commitAllowingStateLoss()
                }
                else -> {
                    cameraPermissionLauncher.launch(CAMERA)
                }
            }

        }


        editorActionButton.setOnClickListener{
            Toast.makeText(requireContext(),"Add images, pdf or txt files to edit",Toast.LENGTH_SHORT).show()
            requireActivity().supportFragmentManager.beginTransaction().replace(R.id.fContainer, PdfEditorFragment(), "pdfEditor").addToBackStack("pdfEditor").commitAllowingStateLoss()
        }

        filePickerAdapter = FilePickerAdapter(requireContext(), files, object :
            OnFileClickListener {
            override fun onClicked(position: Int) {
                openEditor(files[position])
            }

            override fun onDownload(position: Int) {
                outFile = files[position]
                createDocument(files[position].name, TYPE_PDF, documentCreatorLauncher)
            }
        })

        filePickerAdapter.setHasStableIds(true)
        homeRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        homeRecyclerView.adapter = filePickerAdapter
        homeRecyclerView.setItemViewCacheSize(30)


        val aboutUs : Button = layout.findViewById(R.id.aboutUs)
        aboutUs.setOnClickListener {
            Toast.makeText(requireContext(),"B Scanner version 1.0",Toast.LENGTH_SHORT).show()
        }*/





    }


    override fun onResume() {
        super.onResume()
        //toggle.syncState()

        files = pdfFilesFromStorageLocation()
        if(files.isNotEmpty()){
            //homeLinearLayout.visibility = View.GONE
            //filePickerAdapter.data(files)
        }
        else{
           //homeLinearLayout.visibility = View.VISIBLE
        }

    }

    @Composable
    fun HomeScreen(
        onCameraClick : () -> Unit,
        onEditorClick: () -> Unit
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

}