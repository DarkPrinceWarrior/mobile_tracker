package com.example.mobile_tracker.presentation.common

sealed interface ContentState<out T> {
    data object Loading : ContentState<Nothing>
    data object Empty : ContentState<Nothing>
    data class Error(val message: String) : ContentState<Nothing>
    data class Data<T>(val value: T) : ContentState<T>
}
