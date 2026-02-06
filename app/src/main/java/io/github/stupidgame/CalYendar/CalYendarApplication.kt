package io.github.stupidgame.CalYendar

import android.app.Application
import io.github.stupidgame.CalYendar.data.CalYendarDatabase

class CalYendarApplication : Application() {
    val database: CalYendarDatabase by lazy { CalYendarDatabase.getDatabase(this) }
}
