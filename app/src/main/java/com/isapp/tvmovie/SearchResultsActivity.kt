package com.isapp.tvmovie

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_SEARCH
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.SearchView
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.squareup.picasso.Picasso
import info.movito.themoviedbapi.TmdbApi
import info.movito.themoviedbapi.TmdbSearch
import info.movito.themoviedbapi.model.MovieDb
import info.movito.themoviedbapi.model.Multi
import info.movito.themoviedbapi.model.tv.TvSeries
import io.reactivex.ObservableOnSubscribe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_searching.*
import java.util.*
import java.util.concurrent.TimeUnit


class SearchResultsActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_searching)
        setSupportActionBar(toolbar)

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the options menu from XML
        menuInflater.inflate(R.menu.menu_search, menu)

        // Get the SearchView and set the searchable configuration
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView = menu.findItem(R.id.action_search).actionView as SearchView

        // Assumes current activity is the searchable activity
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        searchView.setIconifiedByDefault(false) // Do not iconify the widget; expand it by default
        searchView.maxWidth = Integer.MAX_VALUE

        io.reactivex.Observable.create(ObservableOnSubscribe<String> { emitter ->
            val listener: SearchView.OnQueryTextListener = object : SearchView.OnQueryTextListener {
                override fun onQueryTextChange(newText: String): Boolean {
                    if (newText.isNotBlank())
                        emitter.onNext(newText)
                    return true
                }

                override fun onQueryTextSubmit(query: String): Boolean {
                    return false
                }
            }
            searchView.setOnQueryTextListener(listener)
        })
                .debounce(350L, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { query ->
                    SearchResultsTask().execute(query)
                }


        return true
    }

    override fun onNewIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_SEARCH -> SearchResultsTask().execute(intent.getStringExtra(SearchManager.QUERY))
        }
    }

    data class ViewHolder(val title: TextView, val year: TextView, val poster: ImageView, val desc: TextView)

    inner class SearchResultsTask : AsyncTask<String?, Int, List<Multi>>() {
        override fun doInBackground(vararg params: String?): List<Multi> {
            val searchApi: TmdbSearch = TmdbApi("f6d561bbe27024bff5e74b6695cf7943").search
            return searchApi
                    .searchMulti(params.first(), Locale.getDefault().language, 1)
                    .results
                    .filter { it.mediaType == Multi.MediaType.TV_SERIES || it.mediaType == Multi.MediaType.MOVIE}
        }

        override fun onPostExecute(results: List<Multi>) {
            val resultsAdapter = object : BaseAdapter() {
                override fun getItem(position: Int): Multi {
                    return results[position]
                }

                override fun getItemId(position: Int): Long {
                    return position.toLong()
                }

                override fun getCount(): Int {
                    return results.count()
                }


                override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                    val layout = convertView
                            ?: layoutInflater.inflate(when (getItemViewType(position)) {
                                0 -> R.layout.movie_item
                                1 -> R.layout.movie_item
                                else -> R.layout.movie_item
                            }, parent, false)

                    if (layout.tag == null) {
                        layout.tag = ViewHolder(layout.findViewById(R.id.movie_title), layout.findViewById(R.id.movie_year), layout.findViewById(R.id.movie_poster), layout.findViewById(R.id.movie_description))
                    }

                    val holder = layout.tag as ViewHolder

                    val item = getItem(position)

                    when(item) {
                        is MovieDb -> {
                            holder.title.text = item.title
                            holder.year.text = item.releaseDate
                            holder.desc.text = item.overview
                            Picasso.get().load(item?.posterPath).into(holder.poster)
                        }
                        is TvSeries -> {
                            holder.title.text = item.originalName
                            holder.year.text = item.firstAirDate
                            holder.desc.text = item.overview
                            Picasso.get().load(item?.posterPath).into(holder.poster)
                        }
                    }

                    return layout
                }

                override fun getViewTypeCount(): Int {
                    return 2
                }

                override fun getItemViewType(position: Int): Int {
                    return when (getItem(position).mediaType) {
                        Multi.MediaType.MOVIE -> 0
                        Multi.MediaType.TV_SERIES -> 1
                        else -> Adapter.IGNORE_ITEM_VIEW_TYPE
                    }
                }
            }

            findViewById<ListView>(R.id.search_results).adapter = resultsAdapter
        }
    }
}
