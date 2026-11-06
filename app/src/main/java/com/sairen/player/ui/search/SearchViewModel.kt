package com.sairen.player.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sairen.player.data.MusicLibrary
import com.sairen.player.data.model.LibrarySnapshot
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Debounced search. Each keystroke restarts a [SEARCH_DEBOUNCE_MS] timer; only
 * the final text triggers a scan, which runs on [Dispatchers.Default] with a
 * cached [SearchIndex] so the main thread never filters thousands of tracks.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val musicLibrary: MusicLibrary,
    private val debounceMs: Long = SEARCH_DEBOUNCE_MS,
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow(SearchResults.Empty)
    val results: StateFlow<SearchResults> = _results.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private var indexedSnapshot: LibrarySnapshot? = null
    private var index: SearchIndex = SearchIndex.Empty

    init {
        viewModelScope.launch {
            combine(
                musicLibrary.state.map { it.library }.distinctUntilChanged(),
                _query.debounce(debounceMs).distinctUntilChanged(),
            ) { snapshot, text -> snapshot to text }
                .mapLatest { (snapshot, text) ->
                    if (text.isBlank()) {
                        SearchResults.Empty.copy(query = text)
                    } else {
                        withContext(computeDispatcher) { indexFor(snapshot).search(text) }
                    }
                }
                .collect { results ->
                    _results.value = results
                    _isSearching.value = false
                }
        }
    }

    fun onQueryChange(text: String) {
        if (text == _query.value) return
        _query.value = text
        _isSearching.value = text.isNotBlank()
        if (text.isBlank()) {
            _results.value = SearchResults.Empty
        }
    }

    fun clear() = onQueryChange("")

    private fun indexFor(snapshot: LibrarySnapshot): SearchIndex {
        if (snapshot !== indexedSnapshot) {
            index = SearchIndex.of(snapshot)
            indexedSnapshot = snapshot
        }
        return index
    }

    companion object {
        const val SEARCH_DEBOUNCE_MS = 300L

        fun factory(
            musicLibrary: MusicLibrary,
            computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { SearchViewModel(musicLibrary, computeDispatcher = computeDispatcher) }
        }
    }
}
