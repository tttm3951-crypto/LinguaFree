package org.linguafree.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** Application class. No analytics, no ads, no network SDKs are initialised here — by design. */
@HiltAndroidApp
class LinguaFreeApp : Application()
