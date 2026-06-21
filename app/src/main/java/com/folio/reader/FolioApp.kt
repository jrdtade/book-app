package com.folio.reader

import android.app.Application
import com.folio.reader.data.BookRepository
import com.folio.reader.data.ReaderPrefsRepository
import com.folio.reader.data.UserPrefsRepository
import com.folio.reader.network.FolioHttpClient
import okhttp3.OkHttpClient

class FolioApp : Application() {
    lateinit var repository: BookRepository
    lateinit var readerPrefsRepository: ReaderPrefsRepository
    lateinit var userPrefsRepository: UserPrefsRepository

    /** Shared OkHttpClient for network-backed sources, with a persistent cookie jar
     *  so sessions established via a source's login WebView carry over to API calls. */
    val httpClient: OkHttpClient by lazy { FolioHttpClient.get(this) }

    override fun onCreate() {
        super.onCreate()
        repository = BookRepository(this)
        readerPrefsRepository = ReaderPrefsRepository(this)
        userPrefsRepository = UserPrefsRepository(this)
    }
}
