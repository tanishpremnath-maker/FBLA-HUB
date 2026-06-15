package com.example.myapplication.ui.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.NewsArticle
import com.example.myapplication.UserProfile
import com.example.myapplication.data.repository.AuthRepository
import com.example.myapplication.data.repository.ConnectionRepository
import com.example.myapplication.data.repository.MessageRepository
import com.example.myapplication.data.repository.NewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Immutable UI state for the News screen. The screen renders purely from this. */
data class NewsUiState(
    val articles: List<NewsArticle> = emptyList(),
    val isLoading: Boolean = false,
    val lastFetchTime: Long = System.currentTimeMillis(),
    val searchQuery: String = "",
    val selectedTag: String = "ALL",
    val connections: List<UserProfile> = emptyList(),
    val isLoadingConnections: Boolean = false
) {
    /** Articles after applying the tag filter and search query. */
    val displayList: List<NewsArticle>
        get() = articles.filter { a ->
            (selectedTag == "ALL" || a.tag == selectedTag) &&
                (searchQuery.isEmpty() ||
                    a.title.contains(searchQuery, ignoreCase = true) ||
                    a.summary.contains(searchQuery, ignoreCase = true))
        }
}

@HiltViewModel
class NewsViewModel @Inject constructor(
    private val newsRepo: NewsRepository,
    private val authRepo: AuthRepository,
    private val connectionRepo: ConnectionRepository,
    private val messageRepo: MessageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewsUiState(articles = newsRepo.hardcodedArticles))
    val uiState: StateFlow<NewsUiState> = _uiState.asStateFlow()

    val allTags = listOf("ALL", "NATIONAL", "CONFERENCE", "HS", "OPPORTUNITY")
    val currentUid: String? get() = authRepo.currentUid

    init {
        // Show hardcoded instantly, then silently refresh in the background.
        viewModelScope.launch {
            try {
                val fetched = newsRepo.fetchFBLANews()
                if (fetched.isNotEmpty()) {
                    _uiState.update { it.copy(articles = fetched, lastFetchTime = System.currentTimeMillis()) }
                }
            } catch (_: Exception) { /* keep hardcoded */ }
        }
    }

    /** Pull-to-refresh / refresh button. [onDone] fires after the fetch completes. */
    fun refresh(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val fetched = newsRepo.fetchFBLANews()
            _uiState.update {
                it.copy(articles = fetched, isLoading = false, lastFetchTime = System.currentTimeMillis())
            }
            onDone()
        }
    }

    fun setSearchQuery(query: String) = _uiState.update { it.copy(searchQuery = query) }
    fun setSelectedTag(tag: String) = _uiState.update { it.copy(selectedTag = tag) }

    /** Loads the current user's connections for the "send to member" picker. */
    fun loadConnections() {
        val uid = authRepo.currentUid ?: return
        if (_uiState.value.connections.isNotEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingConnections = true) }
            val loaded = connectionRepo.loadConnections(uid)
            _uiState.update { it.copy(connections = loaded, isLoadingConnections = false) }
        }
    }

    fun clearConnections() = _uiState.update { it.copy(connections = emptyList()) }

    /** Sends [article] to [member] as a direct message. */
    fun sendArticleToMember(member: UserProfile, article: NewsArticle) {
        val uid = authRepo.currentUid ?: return
        messageRepo.sendArticle(uid, authRepo.currentUserName, member, article)
    }
}
