package io.github.stupidgame.calyendar.data

import androidx.room.Entity
import androidx.room.PrimaryKey

sealed interface calyendarItem {
    val year: Int
    val month: Int
    val day: Int
}

@Entity(tableName = "goals", primaryKeys = ["year", "month", "day"])
data class Goal(
    val name: String,
    val amount: Double,
    override val year: Int,
    override val month: Int,
    override val day: Int
) : calyendarItem

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val description: String,
    val amount: Double,
    override val year: Int,
    override val month: Int,
    override val day: Int
) : calyendarItem

@Entity(tableName = "incomes")
data class Income(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val description: String,
    val amount: Double,
    override val year: Int,
    override val month: Int,
    override val day: Int
) : calyendarItem
