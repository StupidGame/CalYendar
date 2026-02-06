package io.github.stupidgame.CalYendar

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
import io.github.stupidgame.CalYendar.data.FinancialGoal
import io.github.stupidgame.CalYendar.data.Transaction
import io.github.stupidgame.CalYendar.data.TransactionType

@Composable
fun AddGoalDialog(
    goal: FinancialGoal?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, amount: Long) -> Unit
) {
    var name by remember { mutableStateOf(goal?.name ?: "") }
    var amount by remember { mutableStateOf(goal?.amount?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (goal == null) "目標を追加" else "目標を編集") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("目標の名前") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { value ->
                        amount = value.filter { it.isDigit() }
                    },
                    label = { Text("目標金額") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    amount.toLongOrNull()?.let { onConfirm(name, it) }
                },
                enabled = amount.toLongOrNull() != null && name.isNotBlank()
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

@Composable
fun AddTransactionDialog(
    transaction: Transaction?,
    type: TransactionType,
    onDismiss: () -> Unit,
    onConfirm: (name: String, amount: Long) -> Unit
) {
    var name by remember { mutableStateOf(transaction?.name ?: "") }
    var amount by remember { mutableStateOf(transaction?.amount?.toString() ?: "") }

    val title = when (type) {
        TransactionType.INCOME -> if (transaction == null) "収入を追加" else "収入を編集"
        TransactionType.EXPENSE -> if (transaction == null) "支出を追加" else "支出を編集"
        else -> ""
    }
    val nameLabel = when (type) {
        TransactionType.INCOME -> "詳細"
        TransactionType.EXPENSE -> "内容"
        else -> ""
    }
    val amountLabel = when (type) {
        TransactionType.INCOME -> "収入額"
        TransactionType.EXPENSE -> "金額"
        else -> ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(nameLabel) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { value ->
                        amount = value.filter { it.isDigit() }
                    },
                    label = { Text(amountLabel) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    amount.toLongOrNull()?.let { onConfirm(name, it) }
                },
                enabled = amount.toLongOrNull() != null && name.isNotBlank()
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
