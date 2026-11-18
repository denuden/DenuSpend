package com.gmail.vondenuelle.denuspend

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

// Need a Context somewhere? Inject it with Hilt's @ApplicationContext instead of a static instance.
@HiltAndroidApp
class DenuSpendApp : Application()
