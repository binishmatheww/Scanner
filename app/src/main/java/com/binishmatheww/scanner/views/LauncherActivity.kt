package com.binishmatheww.scanner.views

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.binishmatheww.scanner.R
import com.binishmatheww.scanner.common.utils.clearTemporaryLocation
import com.binishmatheww.scanner.common.utils.storageLocation
import com.binishmatheww.scanner.common.utils.temporaryLocation
import kotlinx.coroutines.launch


class LauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)

        lifecycleScope.launch {

            temporaryLocation(this@LauncherActivity)

            storageLocation(this@LauncherActivity)

        }

    }

    override fun onDestroy() {
        clearTemporaryLocation(this)
        super.onDestroy()
    }


}