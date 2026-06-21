package com.folio.reader

import android.app.Application
import com.folio.reader.data.BookRepository
import com.folio.reader.data.ReaderPrefsRepository
import com.folio.reader.data.UserPrefsRepository
import com.folio.reader.extension.ExtensionManager
import com.folio.reader.network.FolioHttpClient
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektModule
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.api.addSingletonFactory

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

        // Real extensions resolve `Injekt.get<Application>()` / `Injekt.get<NetworkHelper>()`
        // themselves (e.g. for login SharedPreferences or a shared OkHttpClient) — these
        // bindings make those calls return working instances instead of throwing.
        Injekt.importModule(
            object : InjektModule {
                override fun InjektRegistrar.registerInjectables() {
                    addSingleton(this@FolioApp as Application)
                    addSingletonFactory { NetworkHelper(this@FolioApp, httpClient) }
                }
            },
        )

        extensionManager = ExtensionManager(this, appScope, httpClient)
        repository = BookRepository(this, extensionManager, appScope)
        readerPrefsRepository = ReaderPrefsRepository(this)
        userPrefsRepository = UserPrefsRepository(this)
    }
}
