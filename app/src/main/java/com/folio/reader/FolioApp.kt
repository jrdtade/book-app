package com.folio.reader

import android.app.Application
import com.folio.reader.data.BookRepository
import com.folio.reader.data.ReaderPrefsRepository
import com.folio.reader.data.UserPrefsRepository
import com.folio.reader.extension.ExtensionManager
import com.folio.reader.network.FolioHttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient

class FolioApp : Application() {
    lateinit var repository: BookRepository
    lateinit var readerPrefsRepository: ReaderPrefsRepository
    lateinit var userPrefsRepository: UserPrefsRepository
    lateinit var extensionManager: ExtensionManager

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** Shared OkHttpClient for network-backed sources, with a persistent cookie jar
     *  so sessions established via a source's login WebView carry over to API calls. */
    val httpClient: OkHttpClient by lazy { FolioHttpClient.get(this) }

    override fun onCreate() {
        super.onCreate()
        extensionManager = ExtensionManager(this, appScope, httpClient)
        repository = BookRepository(this, extensionManager, appScope)
        readerPrefsRepository = ReaderPrefsRepository(this)
        userPrefsRepository = UserPrefsRepository(this)
    }
}
