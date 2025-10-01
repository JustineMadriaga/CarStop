package ie.tus.mad.carstop1.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {
    private val _userName = MutableLiveData("User's Name")
    val userName: LiveData<String> get() = _userName
}