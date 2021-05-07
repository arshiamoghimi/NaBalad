package ir.sambal.nabalad

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.room.Room
import com.droidnet.DroidListener
import com.droidnet.DroidNet
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mapbox.android.core.permissions.PermissionsManager
import ir.sambal.nabalad.database.AppDatabase
import ir.sambal.nabalad.database.entities.Bookmark

class MainActivity : AppCompatActivity(), DroidListener {

    private var mDroidNet: DroidNet? = null

    private lateinit var db: AppDatabase

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

        mapsFragment = MapsFragment(db) { itemId ->
            val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
            bottomNavigation.selectedItemId = itemId
        }
        setCurrentFragment(mapsFragment!!)

        bookmarkFragment = BookmarkFragment(db) { bookmark: Bookmark ->
            mapsFragment?.showBookmark(bookmark)
            val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
            bottomNavigation.selectedItemId = R.id.maps_menu_item
        }

        settingFragment = SettingFragment(db)

        setContentView(R.layout.activity_main)

        supportActionBar?.hide()

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigation.setOnNavigationItemSelectedListener {
            changePage(it.itemId)
            true
        }
    }

    private fun changePage(itemId: Int) {
        val title: String?
        when (itemId) {
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
    }

    override fun onBackPressed() {
        if (mapsFragment?.isVisible == true) {
            super.onBackPressed()
        }
        mapsFragment?.let { setCurrentFragment(it) }
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigation.selectedItemId = R.id.maps_menu_item
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
            Toast.makeText(applicationContext, R.string.no_network, Toast.LENGTH_LONG).show()
        }
    }
}