package com.binishmatheww.scanner.views.fragments

import android.Manifest.permission.CAMERA
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.documentfile.provider.DocumentFile
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.binishmatheww.scanner.R
import com.binishmatheww.scanner.views.adapters.FilePickerAdapter
import com.binishmatheww.scanner.views.listeners.OnFileClickListener
import com.binishmatheww.scanner.utils.*
import com.binishmatheww.scanner.views.utils.TYPE_PDF
import com.binishmatheww.scanner.views.utils.createDocument
import com.binishmatheww.scanner.views.utils.openEditor
import com.binishmatheww.scanner.views.utils.pdfFilesFromStorageLocation
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import java.io.File
import java.io.FileOutputStream


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
                requireActivity().supportFragmentManager.beginTransaction().replace(R.id.fContainer, CameraFragment(), "camera").addToBackStack("camera").commitAllowingStateLoss()
            } else {
                Toast.makeText(requireContext(),"Please enable camera permission",Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(layout: View, savedInstanceState: Bundle?) {
        super.onViewCreated(layout, savedInstanceState)

        cameraActionButton = layout.findViewById(R.id.cameraActionButton)
        editorActionButton = layout.findViewById(R.id.editorActionButton)

        homeRecyclerView = layout.findViewById(R.id.homeRecyclerView)
        homeLinearLayout = layout.findViewById(R.id.homeLinearLayout)

        drawerLayout = layout.findViewById(R.id.drawerLayout)
        drawer = layout.findViewById(R.id.drawer)

        toggle = ActionBarDrawerToggle(requireActivity(),drawerLayout,layout.findViewById(R.id.toolBar),R.string.open,R.string.close)

        drawerLayout?.addDrawerListener(toggle)

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
        }





    }


    override fun onResume() {
        super.onResume()
        toggle.syncState()

        files = pdfFilesFromStorageLocation()
        if(files.isNotEmpty()){
            homeLinearLayout.visibility = View.GONE
            filePickerAdapter.data(files)
        }
        else{
            homeLinearLayout.visibility = View.VISIBLE
        }

    }

}