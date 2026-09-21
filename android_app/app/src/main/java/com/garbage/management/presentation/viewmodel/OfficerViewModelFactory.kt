package com.garbage.management.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.garbage.management.di.AppContainer

class OfficerViewModelFactory(
    private val appContainer: AppContainer
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OfficerViewModel::class.java)) {
            return OfficerViewModel(
                getAllComplaintsUseCase = appContainer.getAllComplaintsUseCase,
                getComplaintByIdUseCase = appContainer.getComplaintByIdUseCase,
                getAvailableDriversUseCase = appContainer.getAvailableDriversUseCase,
                assignDriverUseCase = appContainer.assignDriverUseCase,
                updateComplaintStatusUseCase = appContainer.updateComplaintStatusUseCase,
                addOfficerNoteUseCase = appContainer.addOfficerNoteUseCase
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
