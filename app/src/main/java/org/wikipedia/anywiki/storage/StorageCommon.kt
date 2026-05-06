package org.wikipedia.anywiki.storage

import kotlinx.serialization.json.Json

internal const val ANYWIKI_PREFS = "anywiki_reader_prefs"

internal val storageJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}
