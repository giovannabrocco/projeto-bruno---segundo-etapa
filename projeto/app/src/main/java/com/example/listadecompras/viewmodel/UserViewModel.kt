package com.example.listadecompras.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.listadecompras.data.FirebaseRepository

class UserViewModel : ViewModel() {

    private val repository = FirebaseRepository()

    private val _loginResult = MutableLiveData<Result<Unit>>()
    val loginResult: LiveData<Result<Unit>> = _loginResult

    private val _registerResult = MutableLiveData<Result<Unit>>()
    val registerResult: LiveData<Result<Unit>> = _registerResult

    private val _resetPasswordResult = MutableLiveData<Result<Unit>>()
    val resetPasswordResult: LiveData<Result<Unit>> = _resetPasswordResult

    fun login(email: String, password: String) {
        repository.loginUser(
            email = email,
            password = password,
            onSuccess = { _loginResult.value = Result.success(Unit) },
            onFailure = { _loginResult.value = Result.failure(it) }
        )
    }

    fun register(email: String, password: String, name: String) {
        repository.registerUser(
            email = email,
            password = password,
            name = name,
            onSuccess = { _registerResult.value = Result.success(Unit) },
            onFailure = { _registerResult.value = Result.failure(it) }
        )
    }

    fun resetPassword(email: String) {
        repository.resetPassword(
            email = email,
            onSuccess = { _resetPasswordResult.value = Result.success(Unit) },
            onFailure = { _resetPasswordResult.value = Result.failure(it) }
        )
    }

    fun logout() {
        repository.logoutUser()
    }

    fun isUserLoggedIn(): Boolean {
        return repository.getCurrentUser() != null
    }
}
