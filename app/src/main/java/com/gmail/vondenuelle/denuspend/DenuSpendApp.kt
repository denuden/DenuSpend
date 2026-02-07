package com.gmail.vondenuelle.denuspend

import android.app.Application
import android.content.Context
import dagger.hilt.android.HiltAndroidApp

//https://gist.github.com/paraya3636/bf8108a75eb49323e56c0c90dd0747e0
@HiltAndroidApp
class DenuSpendApp : Application()  {
    init {
        instance = this
    }

    companion object {
        private var instance: DenuSpendApp? = null

        fun applicationContext() : Context {
            return instance!!.applicationContext
        }
    }

    override fun onCreate() {
        super.onCreate()
        // initialize for any

        // Use ApplicationContext.
        // example: SharedPreferences etc...
        val context: Context = DenuSpendApp.applicationContext()
    }
}