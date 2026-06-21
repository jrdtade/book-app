package com.folio.reader

import android.app.Application
import com.folio.reader.data.BookRepository
import com.folio.reader.data.ReaderPrefsRepository
import com.folio.reader.data.UserPrefsRepository

class FolioApp : Application() {
    lateinit var repository: BookRepository
    lateinit var readerPrefsRepository: ReaderPrefsRepository
    lateinit var userPrefsRepository: UserPrefsRepository

    override fun onCreate() {
        super.onCreate()
        repository = BookRepository(this)
        readerPrefsRepository = ReaderPrefsRepository(this)
        userPrefsRepository = UserPrefsRepository(this)
    }
}
