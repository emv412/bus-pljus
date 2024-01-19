package com.buspljus

import android.app.Activity
import android.app.SearchManager
import android.content.Context
import android.view.Menu
import android.widget.SearchView

class Pretrazivanje : Activity() {

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.pretraga, menu)

        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView = menu.findItem(R.id.action_search).actionView as SearchView
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))

        return true
    }
}