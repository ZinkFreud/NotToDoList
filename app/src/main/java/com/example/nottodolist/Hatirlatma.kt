package com.example.nottodolist

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

/* Gunluk hatirlatma.

   Neden var: uygulama dogasi geregi az aciliyor -- insan ancak
   bozdugunda aciyor. Aksam bir hatirlatma hem "hala dayaniyorsun"
   demek icin iyi bir sebep, hem de uygulamayi hatirlatiyor. */

const val KANAL_ID = "gunluk_hatirlatma"
private const val IS_ADI = "gunluk_hatirlatma_isi"
private const val BILDIRIM_ID = 1001
const val HATIRLATMA_SAATI = 20   // aksam 8

/* Bildirim kanali. Android 8'den itibaren sart; kanal yoksa bildirim
   hic gorunmuyor ve hata da vermiyor. */
fun kanaliKur(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val yonetici = context.getSystemService(NotificationManager::class.java) ?: return
    if (yonetici.getNotificationChannel(KANAL_ID) != null) return
    val kanal = NotificationChannel(
        KANAL_ID,
        "Günlük hatırlatma",
        NotificationManager.IMPORTANCE_DEFAULT
    )
    kanal.description = "Akşamları ne kadar dayandığını hatırlatır."
    yonetici.createNotificationChannel(kanal)
}

/* Bildirim izni var mi. Android 13 oncesinde izin diye bir sey yok,
   o yuzden dogrudan true. */
fun bildirimIzniVar(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(
        context, Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED
}

/* Simdiden sonraki ilk HATIRLATMA_SAATI'ne kac milisaniye var. */
private fun ilkGecikmeMs(): Long {
    val simdi = Calendar.getInstance()
    val hedef = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, HATIRLATMA_SAATI)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    if (!hedef.after(simdi)) hedef.add(Calendar.DAY_OF_YEAR, 1)
    return hedef.timeInMillis - simdi.timeInMillis
}

fun hatirlatmayiKur(context: Context) {
    kanaliKur(context)
    val istek = PeriodicWorkRequestBuilder<HatirlatmaWorker>(1, TimeUnit.DAYS)
        .setInitialDelay(ilkGecikmeMs(), TimeUnit.MILLISECONDS)
        .build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        IS_ADI,
        ExistingPeriodicWorkPolicy.UPDATE,
        istek
    )
}

fun hatirlatmayiKaldir(context: Context) {
    WorkManager.getInstance(context).cancelUniqueWork(IS_ADI)
}

/* Butun gunlerdeki maddeler arasinda en uzun suredir dayanilani bulur.
   Bildirimde gosterilecek cumleyi dondurur; madde yoksa null. */
suspend fun hatirlatmaMetni(context: Context): String? {
    val gunler = listOf(
        "Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar"
    )
    var enIyi: NotToDoItem? = null
    var enUzun = -1L
    val simdi = System.currentTimeMillis()
    for (gun in gunler) {
        val maddeler = DataStoreManager.getItems(context, gun).first()
        for (madde in maddeler) {
            val sure = simdi - madde.createdAt
            if (sure > enUzun) { enUzun = sure; enIyi = madde }
        }
    }
    val madde = enIyi ?: return null
    return "${madde.text} — ${kisaSure(enUzun)} dayanıyorsun"
}

/* Bildirim icin kisa sure metni. dayanmaMetni() saniyeye kadar
   yaziyor; bildirimde "3 gun 4 saat 5 dk 12 sn" cok uzun kaciyor. */
fun kisaSure(ms: Long): String {
    val dakika = ms / 60000
    val saat = dakika / 60
    val gun = saat / 24
    return when {
        gun > 0    -> "$gun gün"
        saat > 0   -> "$saat saat"
        dakika > 0 -> "$dakika dakika"
        else       -> "yeni başladın"
    }
}

fun bildirimGoster(context: Context, metin: String) {
    if (!bildirimIzniVar(context)) return
    kanaliKur(context)

    val ac = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val dokunma = PendingIntent.getActivity(
        context, 0, ac,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    val bildirim = NotificationCompat.Builder(context, KANAL_ID)
        .setSmallIcon(R.drawable.ic_bildirim)
        .setContentTitle("NotToDo")
        .setContentText(metin)
        .setStyle(NotificationCompat.BigTextStyle().bigText(metin))
        .setContentIntent(dokunma)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()

    try {
        NotificationManagerCompat.from(context).notify(BILDIRIM_ID, bildirim)
    } catch (e: SecurityException) {
        // Izin calisma aninda geri alinmis olabilir; sessizce geciyoruz.
    }
}

class HatirlatmaWorker(
    private val appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!DataStoreManager.hatirlatmaAcik(appContext).first()) return Result.success()
        val metin = hatirlatmaMetni(appContext) ?: "Bugün nelerden uzak duracaksın?"
        bildirimGoster(appContext, metin)
        return Result.success()
    }
}
