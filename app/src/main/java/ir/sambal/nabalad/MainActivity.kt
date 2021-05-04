package ir.sambal.nabalad

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.room.Room
import com.droidnet.DroidListener
import com.droidnet.DroidNet
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mapbox.android.core.permissions.PermissionsManager
import ir.sambal.nabalad.database.AppDatabase

class MainActivity : AppCompatActivity(), DroidListener {

    private var mDroidNet: DroidNet? = null

    private var db: AppDatabase? = null

    private var mapsFragment: MapsFragment? = null
    private var bookmarkFragment: BookmarkFragment? = null
    private var settingFragment: SettingFragment? = null

    override fun onResume() {
        super.onResume()
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigation.selectedItemId = R.id.maps_menu_item
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DroidNet.init(this)
        mDroidNet = DroidNet.getInstance()
        mDroidNet?.addInternetConnectivityListener(this)

        db = Room.databaseBuilder(
            this,
            AppDatabase::class.java, "data"
        ).fallbackToDestructiveMigration().build()

        askRequiredPermissions()

        mapsFragment = MapsFragment.newInstance()
        setCurrentFragment(mapsFragment!!)

        bookmarkFragment = BookmarkFragment.newInstance(db!!)

        settingFragment = SettingFragment.newInstance()

        setContentView(R.layout.activity_main)

        supportActionBar?.hide()

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigation.setOnNavigationItemSelectedListener {
            val title: String?
            when (it.itemId) {
                R.id.maps_menu_item -> {
                    setCurrentFragment(mapsFragment!!)
                    title = null
                }
                R.id.bookmark_menu_item -> {
                    setCurrentFragment(bookmarkFragment!!)
                    title = getString(R.string.bookmark)
                }
                R.id.settings_menu_item -> {
                    setCurrentFragment(settingFragment!!)
                    title = getString(R.string.setting)
                }
                else -> {
                    setCurrentFragment(mapsFragment!!)
                    title = null
                }
            }
            if (title != null) {
                supportActionBar?.show()
                supportActionBar?.title = title
            } else {
                supportActionBar?.hide()
            }
            true
        }
    }

    private fun setCurrentFragment(fragment: Fragment) =
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.mainFragment, fragment)
            commit()
        }


    private fun askRequiredPermissions() {
        val permissions: Array<String> = getNotGivenPermissions().toTypedArray()
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions, 0)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            mapsFragment?.locationPermissionGiven()
        }
    }

    private fun getNotGivenPermissions(): MutableList<String> {
        val permissions = mutableListOf<String>()
        if (!isPermissionGiven(android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            permissions.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (!isPermissionGiven(android.Manifest.permission.INTERNET)) {
            permissions.add(android.Manifest.permission.INTERNET)
        }
        return permissions
    }

    private fun isPermissionGiven(permission: String): Boolean {
        val result = ActivityCompat.checkSelfPermission(this.applicationContext, permission)
        return result == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        mDroidNet?.removeInternetConnectivityChangeListener(this)
    }

    override fun onInternetConnectivityChanged(isConnected: Boolean) {
        if (!isConnected) {
//            TODO("Ask user for internet")
        }
    }
}