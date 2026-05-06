package org.wikipedia

import androidx.core.content.FileProvider
import work.czzzz.anywiki.R

/**
 * According to the documentation:
 * "It is possible to use FileProvider directly instead of extending it. However, this is not
 * reliable and will causes (sic) crashes on some devices."
 * https://developer.android.com/reference/androidx/core/content/FileProvider
 */
class WikipediaFileProvider : FileProvider(R.xml.file_paths)
