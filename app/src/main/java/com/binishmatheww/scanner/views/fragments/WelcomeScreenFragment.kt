package com.binishmatheww.scanner.views.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.binishmatheww.scanner.R
import com.binishmatheww.scanner.common.theme.AppTheme

class WelcomeScreenFragment : Fragment() {


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

        return ComposeView(context = layoutInflater.context).apply {
            setContent {
                WelcomeScreen()
            }
        }

    }

    override fun onResume() {
        super.onResume()

        Handler(Looper.getMainLooper())
            .postDelayed({
                activity?.findNavController(R.id.navigationController)?.apply {
                    navigate(R.id.action_welcomeScreenFragment_to_homeFragment)
                }
            }, 2000
            )

    }

    @Composable
    private fun WelcomeScreen() {

        AppTheme.ScannerTheme {

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
                    painter = painterResource(id = R.drawable.scanner),
                    contentDescription = LocalContext.current.getString(R.string.appName),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                )

            }

        }

    }

}