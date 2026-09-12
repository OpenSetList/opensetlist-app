package com.opensetlist.app.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class SearchableListState(initSortIndex: Int = 0) {
    var searchQuery by mutableStateOf("")
    var sortIndex by mutableStateOf(initSortIndex)
    val listState = LazyListState()
}