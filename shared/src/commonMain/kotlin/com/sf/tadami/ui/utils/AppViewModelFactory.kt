package com.sf.tadami.ui.utils

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import kotlin.reflect.KClass

/**
 * Makes the view models of the shared screens, with the SavedStateHandle of their screen when their
 * constructor takes one, as Android's default factory does: the desktop's navigation has no factory
 * that makes any.
 */
object AppViewModelFactory : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
        val type = modelClass.java
        val withHandle = type.constructors.firstOrNull { it.parameterTypes.contentEquals(arrayOf(SavedStateHandle::class.java)) }
        @Suppress("UNCHECKED_CAST")
        return if (withHandle != null) {
            withHandle.newInstance(extras.createSavedStateHandle()) as T
        } else {
            type.getDeclaredConstructor().newInstance()
        }
    }
}
