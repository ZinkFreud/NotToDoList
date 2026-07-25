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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.Info
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.ExperimentalMaterial3Api

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NotToDoListTheme {
                var ayarlarAcik by remember { mutableStateOf(false) }
                var gecmisAcik by remember { mutableStateOf(false) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    floatingActionButton = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            FloatingActionButton(
                                onClick = { gecmisAcik = true },
                                containerColor = Color(0xFF2C2C2E),
                                contentColor = Color.White
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "Geçmiş"
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            FloatingActionButton(
                                onClick = { ayarlarAcik = true },
                                containerColor = Color(0xFF2C2C2E),
                                contentColor = Color.White
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Hakkımızda"
                                )
                            }
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
                if (gecmisAcik) {
                    GecmisPenceresi(onDismiss = { gecmisAcik = false })
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val temelGunler = listOf(
        "Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar"
    )
    val baslangic = temelGunler.indexOf(bugununGunu())
    val days = temelGunler.drop(baslangic) + temelGunler.take(baslangic)

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
                .width(100.dp)
                .background(Color(0xFF2C2C2E))
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
                    val kayma by animateDpAsState(if (isSelected) 10.dp else 0.dp)
                    Text(
                        text = kisaGun(day),
                        fontFamily = SpaceGrotesk,
                        fontSize = 24.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color(0xFF2C2C2E) else Color(0xFF8E8E93),
                        modifier = Modifier.offset(x = kayma)
                    )

                }
                HorizontalDivider(color = Color(0xFF48484A))
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
            onEdit = { item, yeniMetin, yeniZaman ->
                val hedefGun = gununAdi(yeniZaman)
                val yeniHal = item.copy(text = yeniMetin, createdAt = yeniZaman)

                scope.launch {
                    if (hedefGun == selectedDay) {
                        // Aynı günde kalıyor: yerinde güncelle
                        val index = items.indexOf(item)
                        if (index != -1) {
                            items[index] = yeniHal
                            DataStoreManager.saveItems(context, selectedDay, items.toList())
                        }
                    } else {
                        // Başka güne taşınıyor: bu günden sil, hedef güne ekle
                        items.remove(item)
                        DataStoreManager.saveItems(context, selectedDay, items.toList())

                        val hedeftekiler = DataStoreManager.getItems(context, hedefGun).first().toMutableList()
                        hedeftekiler.add(0, yeniHal)
                        DataStoreManager.saveItems(context, hedefGun, hedeftekiler)
                    }
                }
            },
            onReset = { item ->
                val bugun = bugununGunu()
                val dayanma = System.currentTimeMillis() - item.createdAt
                val gecmisKaydi = item.copy(dayandiMs = dayanma)
                val yeniHal = item.copy(createdAt = System.currentTimeMillis(), dayandiMs = 0L)

                scope.launch {
                    // 1) Geçmişe kopya at
                    DataStoreManager.addToHistory(context, gecmisKaydi)

                    if (selectedDay == bugun) {
                        // Zaten bugündeyiz: sil ve en üste taşı
                        items.remove(item)
                        items.add(0, yeniHal)
                        DataStoreManager.saveItems(context, selectedDay, items.toList())
                    } else {
                        // Başka gündeyiz: bu günden sil, bugüne ekle
                        items.remove(item)
                        DataStoreManager.saveItems(context, selectedDay, items.toList())

                        val bugunkuler = DataStoreManager.getItems(context, bugun).first().toMutableList()
                        bugunkuler.add(0, yeniHal)
                        DataStoreManager.saveItems(context, bugun, bugunkuler)
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayPage(
    day: String,
    tik: Long,
    items: SnapshotStateList<NotToDoItem>,
    onAdd: (String) -> Unit,
    onDelete: (NotToDoItem) -> Unit,
    onEdit: (NotToDoItem, String, Long) -> Unit,
    onReset: (NotToDoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var newItem by remember { mutableStateOf("") }
    var editingItem by remember { mutableStateOf<NotToDoItem?>(null) }
    var editingText by remember { mutableStateOf("") }
    var editingTime by remember { mutableStateOf(0L) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var resetItem by remember { mutableStateOf<NotToDoItem?>(null) }
    var deleteItem by remember { mutableStateOf<NotToDoItem?>(null) }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(day) {
        newItem = ""
        focusManager.clearFocus()
    }

    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = day,
            fontFamily = SpaceGrotesk, fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C2C2E)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newItem,
                onValueChange = { newItem = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ekle") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF2C2C2E),
                    unfocusedBorderColor = Color(0xFF8E8E93),
                    cursorColor = Color(0xFF2C2C2E)
                )
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
                    containerColor = Color(0xFF2C2C2E)
                )
            ) {
                Text("+", fontFamily = SpaceGrotesk, fontSize = 24.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            for (item in items) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE5E5EA)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.text,
                                fontFamily = SpaceGrotesk,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF2C2C2E),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                editingItem = item
                                editingText = item.text
                                editingTime = item.createdAt
                            }) {
                                Icon(Icons.Default.Edit, "Düzenle", tint = Color(0xFF2C2C2E))
                            }
                            IconButton(onClick = { deleteItem = item }) {
                                Icon(Icons.Default.Delete, "Sil", tint = Color(0xFF2C2C2E))
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = run { tik; sureMetni(item.createdAt) },
                            fontFamily = SpaceGrotesk,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2C2C2E)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { resetItem = item },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFE5E5EA))
                        ) {
                            Text(
                                text = "Bozdum/Resetle",
                                fontFamily = SpaceGrotesk,
                                fontSize = 14.sp,
                                color = Color(0xFF2C2C2E)
                            )
                        }
                    }
                }
            }
        }
    }
    if (editingItem != null) {
        AlertDialog(
            onDismissRequest = { editingItem = null },
            title = { Text("Düzenle", fontFamily = SpaceGrotesk) },
            text = {
                Column {
                    OutlinedTextField(
                        value = editingText,
                        onValueChange = { editingText = it },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2C2C2E),
                            unfocusedBorderColor = Color(0xFF8E8E93),
                            cursorColor = Color(0xFF2C2C2E)
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Başlangıç:",
                        fontFamily = SpaceGrotesk,
                        fontSize = 13.sp,
                        color = Color(0xFF8E8E93)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFE5E5EA))
                    ) {
                        Text(
                            text = tarihMetni(editingTime),
                            fontFamily = SpaceGrotesk,
                            fontSize = 14.sp,
                            color = Color(0xFF2C2C2E)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editingText.isNotBlank()) {
                            onEdit(editingItem!!, editingText.trim(), editingTime)
                        }
                        editingItem = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2E))
                ) {
                    Text("Kaydet", fontFamily = SpaceGrotesk)
                }
            },
            dismissButton = {
                Button(
                    onClick = { editingItem = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E8E93))
                ) {
                    Text("İptal", fontFamily = SpaceGrotesk)
                }
            }
        )
    }
    if (showDatePicker) {
        val dateState = rememberDatePickerState(initialSelectedDateMillis = editingTime)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        val secilenGun = dateState.selectedDateMillis ?: editingTime
                        // Seçilen günün tarihini al, saati koru
                        val eski = java.util.Calendar.getInstance().apply { timeInMillis = editingTime }
                        val yeni = java.util.Calendar.getInstance().apply { timeInMillis = secilenGun }
                        yeni.set(java.util.Calendar.HOUR_OF_DAY, eski.get(java.util.Calendar.HOUR_OF_DAY))
                        yeni.set(java.util.Calendar.MINUTE, eski.get(java.util.Calendar.MINUTE))
                        editingTime = yeni.timeInMillis
                        showDatePicker = false
                        showTimePicker = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2E))
                ) {
                    Text("İleri", fontFamily = SpaceGrotesk)
                }
            }
        ) {
            DatePicker(state = dateState)
        }
    }

    if (showTimePicker) {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = editingTime }
        val timeState = rememberTimePickerState(
            initialHour = cal.get(java.util.Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(java.util.Calendar.MINUTE),
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Saat seç", fontFamily = SpaceGrotesk) },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                Button(
                    onClick = {
                        val yeni = java.util.Calendar.getInstance().apply {
                            timeInMillis = editingTime
                            set(java.util.Calendar.HOUR_OF_DAY, timeState.hour)
                            set(java.util.Calendar.MINUTE, timeState.minute)
                        }
                        // Gelecek zaman seçilmişse şimdiye çek
                        editingTime = minOf(yeni.timeInMillis, System.currentTimeMillis())
                        showTimePicker = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2E))
                ) {
                    Text("Tamam", fontFamily = SpaceGrotesk)
                }
            }
        )
    }
    if (resetItem != null) {
        AlertDialog(
            onDismissRequest = { resetItem = null },
            title = { Text("Sayacı sıfırla", fontFamily = SpaceGrotesk) },
            text = {
                Text(
                    "\"${resetItem!!.text}\" sayacı sıfırlanacak. Emin misin?",
                    fontFamily = SpaceGrotesk
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReset(resetItem!!)
                        resetItem = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2E))
                ) {
                    Text("Sıfırla", fontFamily = SpaceGrotesk)
                }
            },
            dismissButton = {
                Button(
                    onClick = { resetItem = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E8E93))
                ) {
                    Text("Vazgeç", fontFamily = SpaceGrotesk)
                }
            }
        )
    }
    if (deleteItem != null) {
        AlertDialog(
            onDismissRequest = { deleteItem = null },
            title = { Text("Sil", fontFamily = SpaceGrotesk) },
            text = {
                Text(
                    "\"${deleteItem!!.text}\" silinecek. Emin misin?",
                    fontFamily = SpaceGrotesk
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(deleteItem!!)
                        deleteItem = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2E))
                ) {
                    Text("Sil", fontFamily = SpaceGrotesk)
                }
            },
            dismissButton = {
                Button(
                    onClick = { deleteItem = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E8E93))
                ) {
                    Text("Vazgeç", fontFamily = SpaceGrotesk)
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
                    text = "Hakkımızda",
                    fontFamily = SpaceGrotesk,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C2C2E)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Bu uygulama öğrenme amaçlı tasarlanmıştır. " +
                            "İstek ve şikayetleriniz için bizimle " +
                            "info@fabrite.ch adresinden iletişime geçebilirsiniz. " +
                            "Teşekkürler.",
                    fontFamily = SpaceGrotesk,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = Color(0xFF2C2C2E),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2E))
                ) {
                    Text("Kapat", fontFamily = SpaceGrotesk)
                }
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
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2E))
    ) {
        Text(text = metin, fontFamily = SpaceGrotesk, fontSize = 16.sp)
    }
}
val SpaceGrotesk = FontFamily(
    Font(R.font.space_grotesk_regular, FontWeight.Normal),
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
    Font(R.font.space_grotesk_bold, FontWeight.Bold)
)

fun dayanmaMetni(ms: Long): String {
    val saniye = ms / 1000
    val gun = saniye / 86400
    val saat = (saniye % 86400) / 3600
    val dakika = (saniye % 3600) / 60
    val sn = saniye % 60

    val parcalar = mutableListOf<String>()
    if (gun > 0) parcalar.add("$gun gün")
    if (saat > 0) parcalar.add("$saat saat")
    if (dakika > 0) parcalar.add("$dakika dk")
    parcalar.add("$sn sn")

    return parcalar.joinToString(" ") + " dayandı"
}

@Composable
fun GecmisPenceresi(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val kayitlar = remember { mutableStateListOf<NotToDoItem>() }

    LaunchedEffect(Unit) {
        val gecmis = DataStoreManager.getHistory(context).first()
        kayitlar.clear()
        kayitlar.addAll(gecmis.reversed())
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFFFAFAFA),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.75f)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Geçmiş",
                        fontFamily = SpaceGrotesk,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C2C2E),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Kapat", tint = Color(0xFF2C2C2E))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (kayitlar.isEmpty()) {
                    Text(
                        text = "Henüz bozulan bir şey yok.",
                        fontFamily = SpaceGrotesk,
                        fontSize = 15.sp,
                        color = Color(0xFF8E8E93)
                    )
                } else {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        for (kayit in kayitlar) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFE5E5EA)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(14.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = kayit.text,
                                            fontFamily = SpaceGrotesk,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF8E8E93),
                                            textDecoration = TextDecoration.LineThrough
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = dayanmaMetni(kayit.dayandiMs),
                                            fontFamily = SpaceGrotesk,
                                            fontSize = 13.sp,
                                            color = Color(0xFF2C2C2E)
                                        )
                                    }
                                    IconButton(onClick = {
                                        val idx = kayitlar.indexOf(kayit)
                                        val gercekIndex = kayitlar.size - 1 - idx
                                        kayitlar.remove(kayit)
                                        scope.launch {
                                            DataStoreManager.removeFromHistory(context, gercekIndex)
                                        }
                                    }) {
                                        Icon(Icons.Default.Delete, "Sil", tint = Color(0xFF8E8E93))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun tarihMetni(ms: Long): String {
    val format = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale("tr"))
    return format.format(java.util.Date(ms))
}

fun gununAdi(ms: Long): String {
    val takvim = java.util.Calendar.getInstance().apply { timeInMillis = ms }
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