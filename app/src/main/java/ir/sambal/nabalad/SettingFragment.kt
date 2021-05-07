package ir.sambal.nabalad

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.switchmaterial.SwitchMaterial
import ir.sambal.nabalad.database.AppDatabase
import ir.sambal.nabalad.database.viewmodel.SettingViewModel


class SettingFragment(private var db: AppDatabase,
) : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_setting, container, false)

        val viewModel = SettingViewModel(db)

        val switch = view.findViewById<SwitchMaterial>(R.id.nightSwitch)
        when (ThemeHelper.currentMode) {
            ThemeHelper.LIGHT_MODE -> {
                switch.isChecked = false
            }
            ThemeHelper.DARK_MODE -> {
                switch.isChecked = true
            }
        }
        switch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                ThemeHelper.applyTheme(ThemeHelper.DARK_MODE)
            } else {
                ThemeHelper.applyTheme(ThemeHelper.LIGHT_MODE)
            }
        }

        val button = view.findViewById<Button>(R.id.deleteDataButton)
        button.setOnClickListener {
            viewModel.deleteDatabase()
            Toast.makeText(context, R.string.success, Toast.LENGTH_LONG).show()
        }
        return view
    }
}