package com.binishmatheww.scanner.views.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.binishmatheww.scanner.R
import com.binishmatheww.scanner.views.screens.CameraScreen
import java.io.Serializable


class CameraFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

        return ComposeView(context = layoutInflater.context).apply {
            setContent {
                CameraScreen{ images ->

                    if (images.isEmpty()) {
                        Toast.makeText(requireContext(), "No images taken", Toast.LENGTH_SHORT).show()
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


}