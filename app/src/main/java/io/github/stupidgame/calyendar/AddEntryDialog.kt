package io.github.stupidgame.calyendar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.stupidgame.calyendar.data.FinancialGoal
import io.github.stupidgame.calyendar.data.Transaction
import io.github.stupidgame.calyendar.data.TransactionType

@Composable
fun AddGoalDialog(
    goal: FinancialGoal?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, amount: Long) -> Unit
) {
    MoneyEntryDialog(
        labels =
            MoneyEntryDialogLabels(
                title = if (goal == null) "目標を追加" else "目標を編集",
                name = "目標の名前",
                amount = "目標金額"
            ),
        initialName = goal?.name.orEmpty(),
        initialAmount = goal?.amount?.toString().orEmpty(),
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}

@Composable
fun AddTransactionDialog(
    transaction: Transaction?,
    type: TransactionType,
    onDismiss: () -> Unit,
    onConfirm: (name: String, amount: Long) -> Unit
) {
    MoneyEntryDialog(
        labels = transactionEntryLabels(type = type, isEditing = transaction != null),
        initialName = transaction?.name.orEmpty(),
        initialAmount = transaction?.amount?.toString().orEmpty(),
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}

private data class MoneyEntryDialogLabels(
    val title: String,
    val name: String,
    val amount: String
)

private fun transactionEntryLabels(
    type: TransactionType,
    isEditing: Boolean
): MoneyEntryDialogLabels =
    when (type) {
        TransactionType.INCOME ->
            MoneyEntryDialogLabels(
                title = if (isEditing) "収入を編集" else "収入を追加",
                name = "詳細",
                amount = "収入額"
            )

        TransactionType.EXPENSE ->
            MoneyEntryDialogLabels(
                title = if (isEditing) "支出を編集" else "支出を追加",
                name = "内容",
                amount = "金額"
            )

        else -> MoneyEntryDialogLabels(title = "", name = "", amount = "")
    }

@Composable
private fun MoneyEntryDialog(
    labels: MoneyEntryDialogLabels,
    initialName: String,
    initialAmount: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, amount: Long) -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var amount by remember(initialAmount) { mutableStateOf(initialAmount) }
    val parsedAmount = amount.toLongOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(labels.title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(labels.name) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { value ->
                        amount = value.filter { it.isDigit() }
                    },
                    label = { Text(labels.amount) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    parsedAmount?.let { onConfirm(name, it) }
                },
                enabled = parsedAmount != null && name.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}
