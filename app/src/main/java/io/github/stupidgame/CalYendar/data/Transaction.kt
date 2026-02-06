package io.github.stupidgame.CalYendar.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val year: Int,
    val month: Int,
    val day: Int,
    val type: TransactionType,
    val name: String,
    val amount: Long,
    val details: String? = null
)

enum class TransactionType {
    GOAL,
    EXPENSE,
    INCOME
}
