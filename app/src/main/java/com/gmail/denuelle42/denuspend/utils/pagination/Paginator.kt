package com.gmail.denuelle42.denuspend.utils.pagination

interface Paginator<Key, Item> {
   suspend fun loadNextItems()
   suspend fun reset()
}