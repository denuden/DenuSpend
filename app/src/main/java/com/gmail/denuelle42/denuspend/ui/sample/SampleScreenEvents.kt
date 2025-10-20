package com.gmail.denuelle42.denuspend.ui.sample

sealed class SampleScreenEvents {
    data class OnGetEvent(val name : String) : SampleScreenEvents()
}