package com.gmail.vondenuelle.denuspend.utils.pagination

interface Paginator<Key, Item> {
   suspend fun loadNextItems()
   suspend fun reset()
}