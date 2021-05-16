package com.binishmatheww.scanner

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.view.Gravity
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.binishmatheww.scanner.fragments.HomeFragment
import com.binishmatheww.scanner.fragments.PdfEditorFragment
import com.binishmatheww.scanner.fragments.SplashScreenFragment
import com.binishmatheww.scanner.utils.*
import com.google.android.material.navigation.NavigationView
import java.io.File


class LauncherActivity : AppCompatActivity() {

    private var onBackPressedTwice = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)


        temporaryLocation(this)
        storageLocation(this)

        if(savedInstanceState == null){
            clearTemporaryLocation(this)
            supportFragmentManager.beginTransaction().replace(R.id.fContainer,SplashScreenFragment(),"splashScreen").addToBackStack("splashScreen").commitAllowingStateLoss()
            Handler().postDelayed({ loadHome(null) },2000)
        }
        else{
            loadHome(supportFragmentManager.findFragmentByTag("home"))
        }
    }

    private fun loadHome(nullableFragment: Fragment?){
        when {
            nullableFragment!=null -> {
                supportFragmentManager
                        .beginTransaction()
                        //.setCustomAnimations(R.animator.fade_in, R.animator.fade_out)
                        .replace(R.id.fContainer, nullableFragment, nullableFragment.tag)
                        .addToBackStack(nullableFragment.tag)
                        .commitAllowingStateLoss()
            }
            else -> {
                supportFragmentManager.beginTransaction()
                        //.setCustomAnimations(R.animator.fade_in, R.animator.fade_out)
                        .replace(R.id.fContainer, HomeFragment(), "home")
                        .addToBackStack("home")
                        .commitAllowingStateLoss()
            }
        }

        //Log.wtf("intent sent to Activity",intent?.scheme)
        val uri = intent?.data
        uri?.let {
            val bundle = Bundle()
            bundle.putString("uri",uri.toString())
            val frag = PdfEditorFragment()
            frag.arguments = bundle
            supportFragmentManager.beginTransaction().replace(R.id.fContainer,frag,"pdfEditor").addToBackStack("pdfEditor").commitAllowingStateLoss()
        }
    }

    override fun onBackPressed() {
        supportFragmentManager.let { sf ->

            if(sf.getBackStackEntryAt(sf.backStackEntryCount-1).name == "home"){

                val f =sf.findFragmentByTag(sf.getBackStackEntryAt(sf.backStackEntryCount-1).name) as HomeFragment?

                if(f?.drawerLayout?.isDrawerOpen(f.drawer!!) == true){
                    f.drawerLayout?.closeDrawer(GravityCompat.START)
                }
                else{
                    if(onBackPressedTwice){
                        finish()
                    }
                    else{
                        Toast.makeText(this,"Press again to exit",Toast.LENGTH_SHORT).show()
                        onBackPressedTwice = true
                        Handler().postDelayed({
                            onBackPressedTwice = false
                        },2000)
                    }
                }

            }
            else{
                super.onBackPressed()
            }
        }
        }

    override fun onDestroy() {
        clearTemporaryLocation(this)
        super.onDestroy()
    }


    }