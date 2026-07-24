package com.example.nottodolist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nottodolist.ui.theme.NotToDoListTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.blur
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NotToDoListTheme {
                var ayarlarAcik by remember { mutableStateOf(false) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = { ayarlarAcik = true },
                            containerColor = Color(0xFF1C1C1E),
                            contentColor = Color.White
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Ayarlar"
                            )
                        }
                    }
                ) { innerPadding ->
                    MainScreen(
                        modifier = Modifier
                            .padding(innerPadding)
                            .blur(if (ayarlarAcik) 10.dp else 0.dp)
                    )
                }

                if (ayarlarAcik) {
                    AyarlarPenceresi(onDismiss = { ayarlarAcik = false })
                }
            }
        }
    }
}
@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val days = listOf(
        "Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar"
    )

    var selectedDay by remember { mutableStateOf(bugununGunu()) }
    // Her saniye artan bir "tik" sayacı - ekranı canlı tutar
    var tik by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            tik++
        }
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Seçili günün maddeleri
    val items = remember { mutableStateListOf<NotToDoItem>() }

    // Seçili gün değişince, o günün kayıtlı verisini yükle
    LaunchedEffect(selectedDay) {
        val saved = DataStoreManager.getItems(context, selectedDay).first()
        items.clear()
        items.addAll(saved)
    }

    Row(modifier = modifier.fillMaxSize()) {
        // SOL SÜTUN: günler
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(140.dp)
                .background(Color(0xFF1C1C1E))
        ) {
            for (day in days) {
                val isSelected = (day == selectedDay)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clickable { selectedDay = day }
                        .background(if (isSelected) Color(0xFFFAFAFA) else Color.Transparent)
                ) {
                    Text(
                        text = kisaGun(day),
                        fontSize = 24.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color(0xFF1C1C1E) else Color(0xFF8E8E93)
                    )

                }
                HorizontalDivider(color = Color(0xFF2C2C2E))
            }
        }

        // SAĞ TARAF
        DayPage(
            day = selectedDay,
            tik = tik,
            items = items,
            onAdd = { text ->
                val newItem = NotToDoItem(text = text, createdAt = System.currentTimeMillis())
                items.add(0, newItem)
                scope.launch {
                    DataStoreManager.saveItems(context, selectedDay, items.toList())
                }
            },
            onDelete = { item ->
                items.remove(item)
                scope.launch {
                    DataStoreManager.saveItems(context, selectedDay, items.toList())
                }
            },
            onEdit = { item, yeniMetin ->
                val index = items.indexOf(item)
                if (index != -1) {
                    items[index] = item.copy(text = yeniMetin)
                    scope.launch {
                        DataStoreManager.saveItems(context, selectedDay, items.toList())
                    }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFAFAFA))
                .drawBehind {
                    val adim = 24.dp.toPx()
                    val cizgiRengi = Color(0xFFEDEDED)
                    var x = 0f
                    while (x < size.width) {
                        drawLine(cizgiRengi, Offset(x, 0f), Offset(x, size.height), 1f)
                        x += adim
                    }
                    var y = 0f
                    while (y < size.height) {
                        drawLine(cizgiRengi, Offset(0f, y), Offset(size.width, y), 1f)
                        y += adim
                    }
                }
                .padding(24.dp)
        )
    }
}

@Composable
fun DayPage(
    day: String,
    tik: Long,
    items: SnapshotStateList<NotToDoItem>,
    onAdd: (String) -> Unit,
    onDelete: (NotToDoItem) -> Unit,
    onEdit: (NotToDoItem, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var newItem by remember { mutableStateOf("") }
    var editingItem by remember { mutableStateOf<NotToDoItem?>(null) }
    var editingText by remember { mutableStateOf("") }

    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = day,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1C1C1E)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newItem,
                onValueChange = { newItem = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ekle") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (newItem.isNotBlank()) {
                        onAdd(newItem.trim())
                        newItem = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1C1C1E)
                )
            ) {
                Text("+", fontSize = 24.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            for (item in items) {
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                ) {
                    // Üst satır: madde başlığı + ikonlar aynı hizada
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "• ${item.text}",
                            fontSize = 18.sp,
                            color = Color(0xFF1C1C1E),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            editingItem = item
                            editingText = item.text
                        }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Düzenle",
                                tint = Color(0xFF1C1C1E)
                            )
                        }
                        IconButton(onClick = { onDelete(item) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Sil",
                                tint = Color(0xFF8E8E93)
                            )
                        }
                    }
                    // Alt satır: sayaç, tam genişlikte, ikonların altından uzar
                    Text(
                        text = run { tik; sureMetni(item.createdAt) },
                        fontSize = 13.sp,
                        color = Color(0xFF8E8E93),
                        modifier = Modifier.padding(start = 16.dp, top = 2.dp)
                    )
                }
            }
        }
    }
    if (editingItem != null) {
        AlertDialog(
            onDismissRequest = { editingItem = null },
            title = { Text("Düzenle") },
            text = {
                OutlinedTextField(
                    value = editingText,
                    onValueChange = { editingText = it }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editingText.isNotBlank()) {
                            onEdit(editingItem!!, editingText.trim())
                        }
                        editingItem = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1C1E))
                ) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                Button(
                    onClick = { editingItem = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E8E93))
                ) {
                    Text("İptal")
                }
            }
        )
    }
}
fun sureMetni(createdAt: Long): String {
    val fark = System.currentTimeMillis() - createdAt
    if (fark < 0) return "az önce"

    val saniye = fark / 1000
    val gun = saniye / 86400
    val saat = (saniye % 86400) / 3600
    val dakika = (saniye % 3600) / 60
    val sn = saniye % 60

    val parcalar = mutableListOf<String>()
    if (gun > 0) parcalar.add("$gun gün")
    if (saat > 0) parcalar.add("$saat saat")
    if (dakika > 0) parcalar.add("$dakika dk")
    parcalar.add("$sn sn")

    return parcalar.joinToString(" ") + " oldu"
}

fun bugununGunu(): String {
    val takvim = java.util.Calendar.getInstance()
    return when (takvim.get(java.util.Calendar.DAY_OF_WEEK)) {
        java.util.Calendar.MONDAY -> "Pazartesi"
        java.util.Calendar.TUESDAY -> "Salı"
        java.util.Calendar.WEDNESDAY -> "Çarşamba"
        java.util.Calendar.THURSDAY -> "Perşembe"
        java.util.Calendar.FRIDAY -> "Cuma"
        java.util.Calendar.SATURDAY -> "Cumartesi"
        java.util.Calendar.SUNDAY -> "Pazar"
        else -> "Pazartesi"
    }
}

fun kisaGun(day: String): String {
    return when (day) {
        "Pazartesi" -> "PZT"
        "Salı" -> "SAL"
        "Çarşamba" -> "ÇRŞ"
        "Perşembe" -> "PRŞ"
        "Cuma" -> "CUM"
        "Cumartesi" -> "CMT"
        "Pazar" -> "PZR"
        else -> day
    }
}
@Composable
fun AyarlarPenceresi(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFFFAFAFA),
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Ayarlar",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1C1E)
                )
                Spacer(modifier = Modifier.height(24.dp))
                AyarButonu("Üyelik") { }
                Spacer(modifier = Modifier.height(12.dp))
                AyarButonu("Talep / Şikayet") { }
                Spacer(modifier = Modifier.height(12.dp))
                AyarButonu("Hakkımızda") { }
            }
        }
    }
}

@Composable
fun AyarButonu(metin: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1C1E))
    ) {
        Text(text = metin, fontSize = 16.sp)
    }
}