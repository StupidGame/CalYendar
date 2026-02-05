package io.github.stupidgame.curyendar

import android.app.Application
import io.github.stupidgame.curyendar.data.CuryendarDatabase

class CuryendarApplication : Application() {
    val database: CuryendarDatabase by lazy { CuryendarDatabase.getDatabase(this) }
}
