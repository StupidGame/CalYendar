package io.github.stupidgame.calyendar

import android.app.Application
import io.github.stupidgame.calyendar.data.calyendarDatabase

class calyendarApplication : Application() {
    val database: calyendarDatabase by lazy { calyendarDatabase.getDatabase(this) }
}
