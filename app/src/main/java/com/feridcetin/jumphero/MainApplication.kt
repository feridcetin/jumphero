package com.feridcetin.jumphero

import android.app.Application
import android.content.Context

class MainApplication : Application() {

    // Uygulama başladığında ilk çalışan metod
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(LocaleHelper.onAttach(base!!))
    }
}