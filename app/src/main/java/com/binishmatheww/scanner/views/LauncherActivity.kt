package com.binishmatheww.scanner.views

import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.binishmatheww.scanner.R
import com.binishmatheww.scanner.views.fragments.HomeFragment
import com.binishmatheww.scanner.views.fragments.PdfEditorFragment
import com.binishmatheww.scanner.views.fragments.SplashScreenFragment
import com.binishmatheww.scanner.views.utils.clearTemporaryLocation
import com.binishmatheww.scanner.views.utils.storageLocation
import com.binishmatheww.scanner.views.utils.temporaryLocation


class LauncherActivity : AppCompatActivity() {

    private var onBackPressedTwice = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)


        temporaryLocation(this)
        storageLocation(this)

        if(savedInstanceState == null){
            clearTemporaryLocation(this)
            supportFragmentManager.beginTransaction().replace(R.id.fContainer,
                SplashScreenFragment(),"splashScreen").addToBackStack("splashScreen").commitAllowingStateLoss()
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