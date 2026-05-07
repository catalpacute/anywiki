package org.wikipedia.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import org.wikipedia.activity.SingleFragmentActivity
import work.czzzz.anywiki.R

class WikiSourcesActivity : SingleFragmentActivity<WikiSourcesFragment>() {
    override fun createFragment(): WikiSourcesFragment {
        return WikiSourcesFragment.newInstance()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.anywiki_settings_sources_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    override fun inflateAndSetContentView() {
        setContentView(R.layout.activity_settings_base)
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, WikiSourcesActivity::class.java)
        }
    }
}
