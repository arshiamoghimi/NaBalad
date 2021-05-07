package ir.sambal.nabalad.database.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.sambal.nabalad.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingViewModel(
    private val db: AppDatabase,
) : ViewModel() {

    fun deleteDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            db.bookmarkDao().nukeTable()
        }
    }
}