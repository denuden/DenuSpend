package com.gmail.vondenuelle.denuspend.utils

/**
 * Multiple the screen width to number of time scrolled, or total number of items
 * Assuming the item is full screenwidth, this is effective to get current number of scrolls
 *
 * Since is was used for carousel, multiplying to a number, increases the number of times
 * it scrolled futher or backwards
 */
fun calculateScrolledDistance(screenWidthPx: Float, scrollCount: Int, multiplier : Int = 1): Float {
    return (screenWidthPx * scrollCount)*multiplier
}