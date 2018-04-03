package com.isapp.tvmovie

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_SEARCH
import android.media.tv.TvView
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
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
import kotlinx.android.synthetic.main.movie_item.view.*
import kotlinx.android.synthetic.main.tv_show_item.view.*
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

    val POSTER_SERVER: String = "https://image.tmdb.org/t/p/original"

    inner class MovieViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bindData(movie: MovieDb) {
            itemView.movie_title.text = movie.title
            itemView.movie_year.text = movie.releaseDate
            itemView.movie_description.text = movie.overview
            Picasso.get().load(POSTER_SERVER + movie.posterPath).into(itemView.movie_poster)
        }
    }

    inner class TvViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bindData(show: TvSeries) {
            itemView.series_title.text = show.originalName
            itemView.series_first_air_date.text = show.firstAirDate
            itemView.series_description.text = show.overview
            Picasso.get().load(POSTER_SERVER + show.posterPath).into(itemView.series_poster)
        }
    }

    inner class SearchResultsTask : AsyncTask<String?, Int, List<Multi>>() {
        override fun doInBackground(vararg params: String?): List<Multi> {
            val searchApi: TmdbSearch = TmdbApi("f6d561bbe27024bff5e74b6695cf7943").search
            return searchApi
                    .searchMulti(params.first(), Locale.getDefault().language, 1)
                    .results
                    .filter { it.mediaType == Multi.MediaType.TV_SERIES || it.mediaType == Multi.MediaType.MOVIE}
        }

        override fun onPostExecute(results: List<Multi>) {
            val resultsAdapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                    val view = layoutInflater.inflate(viewType, parent, false)
                    return when(viewType) {
                        R.layout.movie_item -> MovieViewHolder(view)
                        R.layout.tv_show_item -> TvViewHolder(view)
                        else -> MovieViewHolder(view)
                    }
                }

                override fun getItemCount(): Int {
                    return results.count()
                }

                override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                    val item = results[position]

                    when(item) {
                        is MovieDb -> (holder as MovieViewHolder).bindData(item)
                        is TvSeries -> (holder as TvViewHolder).bindData(item)
                    }
                }

                override fun getItemViewType(position: Int): Int {
                    return when (results[position].mediaType) {
                        Multi.MediaType.MOVIE -> R.layout.movie_item
                        Multi.MediaType.TV_SERIES -> R.layout.tv_show_item
                        else -> Adapter.IGNORE_ITEM_VIEW_TYPE
                    }
                }
            }

            findViewById<RecyclerView>(R.id.search_results).apply {
                isNestedScrollingEnabled = false
                layoutManager = LinearLayoutManager(applicationContext)
                adapter = resultsAdapter
            }
        }
    }
}
