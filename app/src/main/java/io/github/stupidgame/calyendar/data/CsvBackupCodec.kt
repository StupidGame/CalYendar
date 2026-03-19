package io.github.stupidgame.calyendar.data

data class CsvBackupData(
    val settings: AppSettings,
    val events: List<Event>,
    val transactions: List<Transaction>,
    val goals: List<FinancialGoal>
)

object CsvBackupCodec {
    private const val recordMeta = "META"
    private const val recordSetting = "SETTING"
    private const val recordEvent = "EVENT"
    private const val recordTransaction = "TRANSACTION"
    private const val recordGoal = "GOAL"
    private const val formatVersion = "1"

    private val header =
        listOf(
            "record_type",
            "id",
            "year",
            "month",
            "day",
            "title",
            "start_time_epoch_ms",
            "end_time_epoch_ms",
            "notification_minutes_before",
            "is_holiday",
            "series_id",
            "notifications",
            "transaction_type",
            "name",
            "amount",
            "details",
            "setting_key",
            "setting_value"
        )

    fun encode(data: CsvBackupData): String {
        return buildString {
            append('\uFEFF')
            appendCsvRow(header)
            appendCsvRow(
                listOf(
                    recordMeta,
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "format_version",
                    formatVersion
                )
            )

            appendSettingRows(data.settings)
            data.events.sortedBy { it.id }.forEach { appendEventRow(it) }
            data.transactions.sortedBy { it.id }.forEach { appendTransactionRow(it) }
            data.goals.sortedBy { it.id }.forEach { appendGoalRow(it) }
        }
    }

    fun decode(csv: String): CsvBackupData {
        val rows = parseCsv(csv)
        require(rows.isNotEmpty()) { "バックアップファイルが空です。" }

        val actualHeader = rows.first().normalizeRow().toMutableList()
        if (actualHeader.isNotEmpty()) {
            actualHeader[0] = actualHeader[0].removePrefix("\uFEFF")
        }
        require(actualHeader == header) { "バックアップファイルの形式に対応していません。" }

        var settings = AppSettings()
        val events = mutableListOf<Event>()
        val transactions = mutableListOf<Transaction>()
        val goals = mutableListOf<FinancialGoal>()

        rows.drop(1).forEachIndexed { index, rawRow ->
            if (rawRow.all { it.isBlank() }) return@forEachIndexed

            val row = rawRow.normalizeRow()
            when (row[columnRecordType]) {
                recordMeta -> {
                    val key = row[columnSettingKey]
                    val value = row[columnSettingValue]
                    if (key == "format_version" && value != formatVersion) {
                        throw IllegalArgumentException("バックアップファイルのバージョンに対応していません。($value)")
                    }
                }
                recordSetting -> {
                    settings =
                        when (row[columnSettingKey]) {
                            "web_cal_url" -> settings.copy(webCalUrl = row[columnSettingValue])
                            "notification_one_day_before" ->
                                settings.copy(
                                    notificationOneDayBefore =
                                        row[columnSettingValue].toBooleanValue()
                                )
                            "notification_one_hour_before" ->
                                settings.copy(
                                    notificationOneHourBefore =
                                        row[columnSettingValue].toBooleanValue()
                                )
                            else -> settings
                        }
                }
                recordEvent -> {
                    events +=
                        Event(
                            id = row[columnId].toLongValue("イベントID", index),
                            year = row[columnYear].toIntValue("イベントの年", index),
                            month = row[columnMonth].toMonthValue(index),
                            day = row[columnDay].toIntValue("イベントの日", index),
                            title = row[columnTitle],
                            startTime = row[columnStartTime].toLongValue("イベントの開始時刻", index),
                            endTime = row[columnEndTime].toLongValue("イベントの終了時刻", index),
                            notificationMinutesBefore =
                                row[columnNotificationMinutes]
                                    .ifBlank { "-1" }
                                    .toLongValue("イベント通知", index),
                            isHoliday = row[columnIsHoliday].toBooleanValue(),
                            seriesId = row[columnSeriesId].ifBlank { null },
                            notifications = row[columnNotifications]
                        )
                }
                recordTransaction -> {
                    transactions +=
                        Transaction(
                            id = row[columnId].toIntValue("取引ID", index),
                            year = row[columnYear].toIntValue("取引の年", index),
                            month = row[columnMonth].toMonthValue(index),
                            day = row[columnDay].toIntValue("取引の日", index),
                            type = row[columnTransactionType].toTransactionType(index),
                            name = row[columnName],
                            amount = row[columnAmount].toLongValue("取引金額", index),
                            details = row[columnDetails].ifBlank { null }
                        )
                }
                recordGoal -> {
                    goals +=
                        FinancialGoal(
                            id = row[columnId].toIntValue("目標ID", index),
                            year = row[columnYear].toIntValue("目標の年", index),
                            month = row[columnMonth].toMonthValue(index),
                            day = row[columnDay].toIntValue("目標の日", index),
                            name = row[columnName],
                            amount = row[columnAmount].toLongValue("目標金額", index)
                        )
                }
                else -> {
                    throw IllegalArgumentException("${index + 2}行目のレコード種別が不正です。")
                }
            }
        }

        return CsvBackupData(
            settings = settings,
            events = events,
            transactions = transactions,
            goals = goals
        )
    }

    private fun StringBuilder.appendSettingRows(settings: AppSettings) {
        appendCsvRow(
            rowWithSettings(
                key = "web_cal_url",
                value = settings.webCalUrl
            )
        )
        appendCsvRow(
            rowWithSettings(
                key = "notification_one_day_before",
                value = settings.notificationOneDayBefore.toString()
            )
        )
        appendCsvRow(
            rowWithSettings(
                key = "notification_one_hour_before",
                value = settings.notificationOneHourBefore.toString()
            )
        )
    }

    private fun StringBuilder.appendEventRow(event: Event) {
        appendCsvRow(
            listOf(
                recordEvent,
                event.id.toString(),
                event.year.toString(),
                (event.month + 1).toString(),
                event.day.toString(),
                event.title,
                event.startTime.toString(),
                event.endTime.toString(),
                event.notificationMinutesBefore.toString(),
                event.isHoliday.toString(),
                event.seriesId.orEmpty(),
                event.notifications,
                "",
                "",
                "",
                "",
                "",
                ""
            )
        )
    }

    private fun StringBuilder.appendTransactionRow(transaction: Transaction) {
        appendCsvRow(
            listOf(
                recordTransaction,
                transaction.id.toString(),
                transaction.year.toString(),
                (transaction.month + 1).toString(),
                transaction.day.toString(),
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                transaction.type.name,
                transaction.name,
                transaction.amount.toString(),
                transaction.details.orEmpty(),
                "",
                ""
            )
        )
    }

    private fun StringBuilder.appendGoalRow(goal: FinancialGoal) {
        appendCsvRow(
            listOf(
                recordGoal,
                goal.id.toString(),
                goal.year.toString(),
                (goal.month + 1).toString(),
                goal.day.toString(),
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                goal.name,
                goal.amount.toString(),
                "",
                "",
                ""
            )
        )
    }

    private fun rowWithSettings(key: String, value: String): List<String> {
        return listOf(
            recordSetting,
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            key,
            value
        )
    }

    private fun StringBuilder.appendCsvRow(values: List<String>) {
        append(values.joinToString(",") { escapeCsvValue(it) })
        append('\n')
    }

    private fun escapeCsvValue(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return if (escaped.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"$escaped\""
        } else {
            escaped
        }
    }

    private fun parseCsv(csv: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var currentRow = mutableListOf<String>()
        val currentField = StringBuilder()
        var insideQuotes = false
        var index = 0

        while (index < csv.length) {
            val char = csv[index]
            if (insideQuotes) {
                when (char) {
                    '"' -> {
                        if (index + 1 < csv.length && csv[index + 1] == '"') {
                            currentField.append('"')
                            index++
                        } else {
                            insideQuotes = false
                        }
                    }
                    else -> currentField.append(char)
                }
            } else {
                when (char) {
                    '"' -> insideQuotes = true
                    ',' -> {
                        currentRow.add(currentField.toString())
                        currentField.clear()
                    }
                    '\n' -> {
                        currentRow.add(currentField.toString())
                        currentField.clear()
                        rows.add(currentRow)
                        currentRow = mutableListOf()
                    }
                    '\r' -> Unit
                    else -> currentField.append(char)
                }
            }
            index++
        }

        require(!insideQuotes) { "バックアップファイルに閉じられていない引用符があります。" }

        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString())
            rows.add(currentRow)
        }

        return rows.filterNot { row -> row.size == 1 && row.first().isBlank() }
    }

    private fun List<String>.normalizeRow(): List<String> {
        return if (size >= header.size) {
            take(header.size)
        } else {
            this + List(header.size - size) { "" }
        }
    }

    private fun String.toBooleanValue(): Boolean {
        return when (trim().lowercase()) {
            "true", "1", "yes", "on" -> true
            "false", "0", "no", "off", "" -> false
            else -> throw IllegalArgumentException("真偽値として解釈できない値です。($this)")
        }
    }

    private fun String.toIntValue(fieldName: String, rowIndex: Int): Int {
        return toIntOrNull()
            ?: throw IllegalArgumentException("${rowIndex + 2}行目の${fieldName}が不正です。")
    }

    private fun String.toLongValue(fieldName: String, rowIndex: Int): Long {
        return toLongOrNull()
            ?: throw IllegalArgumentException("${rowIndex + 2}行目の${fieldName}が不正です。")
    }

    private fun String.toMonthValue(rowIndex: Int): Int {
        val month = toIntValue("月", rowIndex)
        require(month in 1..12) { "${rowIndex + 2}行目の月は1から12の間で指定してください。" }
        return month - 1
    }

    private fun String.toTransactionType(rowIndex: Int): TransactionType {
        return runCatching { TransactionType.valueOf(uppercase()) }
            .getOrElse {
                throw IllegalArgumentException("${rowIndex + 2}行目の取引種別が不正です。")
            }
    }

    private const val columnRecordType = 0
    private const val columnId = 1
    private const val columnYear = 2
    private const val columnMonth = 3
    private const val columnDay = 4
    private const val columnTitle = 5
    private const val columnStartTime = 6
    private const val columnEndTime = 7
    private const val columnNotificationMinutes = 8
    private const val columnIsHoliday = 9
    private const val columnSeriesId = 10
    private const val columnNotifications = 11
    private const val columnTransactionType = 12
    private const val columnName = 13
    private const val columnAmount = 14
    private const val columnDetails = 15
    private const val columnSettingKey = 16
    private const val columnSettingValue = 17
}
