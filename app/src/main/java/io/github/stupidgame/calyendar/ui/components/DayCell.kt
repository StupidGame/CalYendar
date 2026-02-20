package io.github.stupidgame.calyendar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.stupidgame.calyendar.data.DayState
import io.github.stupidgame.calyendar.data.TransactionType
import java.util.Calendar

@Composable
fun DayCell(dayState: DayState, year: Int, month: Int, onClick: () -> Unit) {
    val predictionDiff = dayState.predictionDiff

    val cardColor = when {
        predictionDiff != null && dayState.goal != null -> {
            getGradientColor(predictionDiff, dayState.goal.amount)
        }
        else -> MaterialTheme.colorScheme.surface
    }
    val contentColor = if (cardColor.luminance() > 0.5f) Color.Black else Color.White

    val calendar = Calendar.getInstance().apply { set(year, month, dayState.dayOfMonth) }
    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

    val dateTextColor = when {
        dayState.isHoliday || dayOfWeek == Calendar.SUNDAY -> Color(0xFFD32F2F)
        dayOfWeek == Calendar.SATURDAY -> Color(0xFF1976D2)
        else -> contentColor
    }

    val today = java.time.LocalDate.now()
    val currentDayDate = java.time.LocalDate.of(year, month + 1, dayState.dayOfMonth)
    val isToday = currentDayDate.isEqual(today)

    Card(
        modifier = Modifier
            .padding(2.dp)
            .aspectRatio(1f)
            .let {
                if (isToday) it.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                else it
            }
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 2.dp, end = 2.dp, bottom = 2.dp, top = 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 日付とイベントの行
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 0.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dayState.dayOfMonth.toString(),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    color = dateTextColor
                )

                // イベント (ドットインジケーター)
                if (dayState.events.isNotEmpty() || dayState.icalEvents.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(1.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(dayState.events.size) {
                            Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        }
                        repeat(dayState.icalEvents.size) {
                            Box(modifier = Modifier.size(4.dp).background(Color.Cyan, CircleShape))
                        }
                    }
                }
            }
            
            // 日付の下の分割エリア
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                // 上部：収入 / 支出
                Box(
                    modifier = Modifier.wrapContentHeight().fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    val income = dayState.transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                    val expense = dayState.transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (income > 0) {
                            Text(
                                "収+%,d".format(income), 
                                color = Color(0xFF2E7D32), 
                                fontSize = 8.sp, 
                                maxLines = 1, 
                                lineHeight = 8.sp, 
                                textAlign = TextAlign.Center
                            )
                        }
                        if (expense > 0) {
                            Text(
                                "支-%,d".format(expense), 
                                color = Color(0xFFC62828), 
                                fontSize = 8.sp, 
                                maxLines = 1, 
                                lineHeight = 8.sp, 
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f).heightIn(min = 4.dp))

                // 下部：予測 / 目標差額
                Box(
                    modifier = Modifier.wrapContentHeight().fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (predictionDiff != null && dayState.goal != null) {
                        val surplus = predictionDiff - dayState.goal.amount
                        val prefix = if (surplus >= 0) "余" else "不"
                        val predictionTextColor = if (surplus >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                        Text(
                            "%s: %,d".format(prefix, kotlin.math.abs(surplus)),
                            color = predictionTextColor,
                            fontSize = 8.sp,
                            maxLines = 1,
                            lineHeight = 8.sp,
                            textAlign = TextAlign.Center
                        )
                    } else if (dayState.goal != null) {
                        Text(
                            "目標: %,d".format(dayState.goal.amount),
                            color = contentColor,
                            fontSize = 8.sp,
                            maxLines = 1,
                            lineHeight = 8.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * 目標達成率に基づいて色を返す。
 * rate = numerator / denominator
 * - rate < 0%: 赤
 * - 0% <= rate < 100%: 黄
 * - rate >= 100%: 緑
 */
fun getGradientColor(numerator: Long, denominator: Long): Color {
    if (denominator <= 0L) {
        return if (numerator >= 0) Color(0xFFA5D6A7) else Color(0xFFEF9A9A)
    }
    val achievementRate = numerator.toFloat() / denominator.toFloat()
    return when {
        achievementRate < 0f -> Color(0xFFEF9A9A)   // パステルレッド
        achievementRate < 1f -> Color(0xFFFFF9C4)   // パステルイエロー
        else -> Color(0xFFA5D6A7)                    // パステルグリーン
    }
}
