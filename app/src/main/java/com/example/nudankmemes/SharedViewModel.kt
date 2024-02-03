package com.example.nudankmemes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {
    private val _keys = MutableLiveData<List<String>>()
    val keys: LiveData<List<String>> get() = _keys

    fun setKeys(keys: List<String>) {
        _keys.value = keys
    }
}