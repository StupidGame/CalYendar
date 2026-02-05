package io.github.stupidgame.curyendar.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import biweekly.Biweekly
import biweekly.component.VEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.model.Calendar
import java.net.URL

class CalendarViewModel : ViewModel() {
    private val _holidays = MutableStateFlow<List<VEvent>>(emptyList())
    val holidays = _holidays.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val url = URL("https://www.officeholidays.com/ics/japan")
                val ical = Biweekly.parse(url.openStream()).first()
                _holidays.value = ical.events
            }
        }
    }

    fun importWebcal(url: String, onResult: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val calendar = CalendarBuilder().build(URL(url).openStream())
                // TODO: Save events to database
                onResult("インポートに成功しました")
            }.onFailure {
                onResult("インポートに失敗しました")
            }
        }
    }
}