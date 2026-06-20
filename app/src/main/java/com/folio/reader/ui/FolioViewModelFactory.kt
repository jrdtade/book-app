package com.folio.reader.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.folio.reader.FolioApp

/** Tiny factory so screen ViewModels can take the repository directly, no DI framework needed. */
class FolioViewModelFactory(private val app: FolioApp) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return modelClass.getConstructor(FolioApp::class.java).newInstance(app)
    }
}

@Composable
fun localFolioApp(): FolioApp {
    val context = LocalContext.current
    return context.applicationContext as FolioApp
}

@Composable
inline fun <reified T : ViewModel> folioViewModel(): T {
    val app = localFolioApp()
    return viewModel(factory = FolioViewModelFactory(app))
}
