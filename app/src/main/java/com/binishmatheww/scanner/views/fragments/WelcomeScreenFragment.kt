package com.binishmatheww.scanner.views.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.findNavController
import com.binishmatheww.scanner.R
import com.binishmatheww.scanner.views.screens.WelcomeScreen

class WelcomeScreenFragment : Fragment() {


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        return ComposeView(context = layoutInflater.context).apply {
            setContent {
                WelcomeScreen {
                    activity?.findNavController(R.id.navigationController)?.apply {
                        navigate(R.id.action_welcomeScreenFragment_to_homeFragment)
                    }
                }
            }
        }

    }

}