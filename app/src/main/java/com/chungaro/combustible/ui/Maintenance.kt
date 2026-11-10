package com.chungaro.combustible.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.chungaro.combustible.alerts.Notifier
import com.chungaro.combustible.alerts.reminderStatus
import com.chungaro.combustible.data.Reminder
import java.time.LocalDate

private const val DAY_MS = 86_400_000L

@Composable
fun MaintenanceSection(
    reminders: List<Reminder>,
    lastOdometer: Double?,
    onEdit: (Reminder) -> Unit,
    onToggleDone: (Reminder) -> Unit,
) {
    val context = LocalContext.current
    var canNotify by remember { mutableStateOf(Notifier.canNotify(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { canNotify = it }
    val today = LocalDate.now().toEpochDay()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (!canNotify && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Ativa as notificações e avisamos-te quando se aproximar cada manutenção.")
                        TextButton(onClick = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                            Text("Ativar notificações")
                        }
                    }
                }
            }
        }
        if (reminders.isEmpty()) {
            item {
                Text(
                    "Sem lembretes. Adiciona a mudança de óleo, a inspeção, o seguro…\n" +
                        "Avisamos-te alguns dias ou cerca de 500 km antes.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
        }
        items(reminders, key = { it.id }) { r ->
            val daysLeft = r.dueEpochDay?.let { it - today }
            val kmLeft = if (r.dueOdometer != null && lastOdometer != null) r.dueOdometer - lastOdometer else null
            val overdue = (daysLeft != null && daysLeft < 0) || (kmLeft != null && kmLeft <= 0)
            val soon = (daysLeft != null && daysLeft <= 7) || (kmLeft != null && kmLeft <= 500)
            val color = when {
                r.done -> MaterialTheme.colorScheme.onSurfaceVariant
                overdue -> MaterialTheme.colorScheme.error
                soon -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Card(Modifier.fillMaxWidth().clickable { onEdit(r) }) {
                Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = r.done, onCheckedChange = { onToggleDone(r) })
                    Column(Modifier.weight(1f)) {
                        Text(
                            r.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = if (r.done) TextDecoration.LineThrough else null,
                        )
                        val parts = listOfNotNull(
                            r.dueEpochDay?.let { formatDate(it) },
                            r.dueOdometer?.let { "${formatNumber(it, 0)} km" },
                        ).joinToString(" · ")
                        Text(parts, style = MaterialTheme.typography.bodySmall)
                        if (!r.done) {
                            Text(
                                reminderStatus(r.dueEpochDay, r.dueOdometer, today, lastOdometer),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = color,
                            )
                        }
                    }
                }
            }
        }
    }
}

private val QUICK_TITLES = listOf("Mudança de óleo", "Inspeção (IPO)", "Seguro", "Pneus", "Revisão", "IUC")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReminderDialog(
    existing: Reminder?,
    vehicleId: Long,
    onDismiss: () -> Unit,
    onSave: (Reminder) -> Unit,
    onDelete: (Reminder) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf(existing?.title ?: "") }
    var dueDay by rememberSaveable { mutableStateOf(existing?.dueEpochDay) }
    var km by rememberSaveable { mutableStateOf(existing?.dueOdometer?.let { editableNumber(it, 0) } ?: "") }
    var notes by rememberSaveable { mutableStateOf(existing?.notes ?: "") }
    var pickDate by rememberSaveable { mutableStateOf(false) }
    val kmValue = parseNumber(km)
    val valid = title.isNotBlank() && (dueDay != null || kmValue != null)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Novo lembrete" else "Editar lembrete") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("O que é preciso fazer?") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    QUICK_TITLES.forEach { t -> AssistChip(onClick = { title = t }, label = { Text(t) }) }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { pickDate = true }, modifier = Modifier.weight(1f)) {
                        Text(dueDay?.let { "Data: ${formatDate(it)}" } ?: "Escolher data")
                    }
                    if (dueDay != null) TextButton(onClick = { dueDay = null }) { Text("Remover") }
                }
                OutlinedTextField(
                    value = km,
                    onValueChange = { v -> km = v.filter { it.isDigit() } },
                    label = { Text("Aos (km)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Indica uma data, uma quilometragem ou ambas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (existing != null) {
                    TextButton(onClick = { onDelete(existing) }) {
                        Text("Eliminar lembrete", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    onSave(
                        Reminder(
                            id = existing?.id ?: 0,
                            vehicleId = existing?.vehicleId ?: vehicleId,
                            title = title.trim(),
                            dueEpochDay = dueDay,
                            dueOdometer = kmValue,
                            done = existing?.done ?: false,
                            notes = notes.trim(),
                        ),
                    )
                },
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )

    if (pickDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (dueDay ?: LocalDate.now().plusMonths(1).toEpochDay()) * DAY_MS,
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { dueDay = Math.floorDiv(it, DAY_MS) }
                    pickDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text("Cancelar") } },
        ) { DatePicker(state = state) }
    }
}
