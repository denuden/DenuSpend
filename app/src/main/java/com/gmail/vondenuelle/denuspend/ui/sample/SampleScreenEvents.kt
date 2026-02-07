package com.gmail.vondenuelle.denuspend.ui.sample

sealed class SampleScreenEvents {
    data class OnGetEvent(val name : String) : SampleScreenEvents()
}