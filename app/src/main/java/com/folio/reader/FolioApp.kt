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
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
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

        // Real extensions (and their shared "keiyoushi.lib.*"/"keiyoushi.utils.*" helper
        // code) resolve these directly via `Injekt.get<T>()`/`by injectLazy()` themselves.
        // Mirrors Mihon's own AppModule registrations exactly (same Json/ProtoBuf config) —
        // a class's static initializer touching Injekt.get<Json>() with no binding throws
        // ExceptionInInitializerError, which is what was happening before this was added.
        Injekt.importModule(
            object : InjektModule {
                override fun InjektRegistrar.registerInjectables() {
                    addSingleton(this@FolioApp as Application)
                    addSingletonFactory { NetworkHelper(this@FolioApp, httpClient) }
                    addSingletonFactory {
                        Json {
                            ignoreUnknownKeys = true
                            explicitNulls = false
                        }
                    }
                    addSingletonFactory<ProtoBuf> { ProtoBuf }
                }
            },
        )

        extensionManager = ExtensionManager(this, appScope, httpClient)
        repository = BookRepository(this, extensionManager, appScope)
        readerPrefsRepository = ReaderPrefsRepository(this)
        userPrefsRepository = UserPrefsRepository(this)
    }
}
