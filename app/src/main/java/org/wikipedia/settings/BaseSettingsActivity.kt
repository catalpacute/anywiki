package org.wikipedia.settings

import android.os.Bundle
import org.wikipedia.activity.SingleFragmentActivity
import work.czzzz.anywiki.R

abstract class BaseSettingsActivity<T : PreferenceLoaderFragment> : SingleFragmentActivity<T>() {
    abstract val title: Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = getString(title)
    }

    override fun inflateAndSetContentView() {
        setContentView(R.layout.activity_settings_base)
    }
}
