package com.example.nottodolist
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

// Telefonun içinde "nottodo_data" adında bir depo oluşturuyoruz
val Context.dataStore by preferencesDataStore(name = "nottodo_data")

// Bir maddeyi temsil eden yapı: metni + eklendiği zaman (milisaniye)
data class NotToDoItem(
    val text: String,
    val createdAt: Long,
    val dayandiMs: Long = 0L
)

object DataStoreManager {

    // Her gün için ayrı bir anahtar (Pazartesi -> "day_Pazartesi" gibi)
    private fun keyForDay(day: String) = stringPreferencesKey("day_$day")

    // Bir günün maddelerini KAYDET
    suspend fun saveItems(context: Context, day: String, items: List<NotToDoItem>) {
        val jsonArray = JSONArray()
        for (item in items) {
            val obj = JSONObject()
            obj.put("text", item.text)
            obj.put("createdAt", item.createdAt)
            obj.put("dayandiMs", item.dayandiMs)
            jsonArray.put(obj)
        }
        context.dataStore.edit { prefs ->
            prefs[keyForDay(day)] = jsonArray.toString()
        }
    }

    // Bir günün maddelerini OKU
    fun getItems(context: Context, day: String): Flow<List<NotToDoItem>> {
        return context.dataStore.data.map { prefs ->
            val raw = prefs[keyForDay(day)] ?: "[]"
            val jsonArray = JSONArray(raw)
            val result = mutableListOf<NotToDoItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                result.add(
                    NotToDoItem(
                        text = obj.getString("text"),
                        createdAt = obj.getLong("createdAt"),
                                dayandiMs = obj.optLong("dayandiMs", 0L)
                    )
                )
            }
            result
        }
    }
    // GEÇMİŞ (bozulan maddeler) için ayrı anahtar
    private val gecmisKey = stringPreferencesKey("gecmis")

    // Geçmişe bir kayıt EKLE
    suspend fun addToHistory(context: Context, item: NotToDoItem) {
        context.dataStore.edit { prefs ->
            val raw = prefs[gecmisKey] ?: "[]"
            val jsonArray = JSONArray(raw)
            val obj = JSONObject()
            obj.put("text", item.text)
            obj.put("createdAt", item.createdAt)
            obj.put("dayandiMs", item.dayandiMs)
            jsonArray.put(obj)
            prefs[gecmisKey] = jsonArray.toString()
        }
    }

    // Geçmişi OKU
    fun getHistory(context: Context): Flow<List<NotToDoItem>> {
        return context.dataStore.data.map { prefs ->
            val raw = prefs[gecmisKey] ?: "[]"
            val jsonArray = JSONArray(raw)
            val result = mutableListOf<NotToDoItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                result.add(
                    NotToDoItem(
                        text = obj.getString("text"),
                        createdAt = obj.getLong("createdAt"),
                        dayandiMs = obj.optLong("dayandiMs", 0L)
                    )
                )
            }
            result
        }
    }

    // GÜNLÜK HATIRLATMA açık mı
    private val hatirlatmaKey = booleanPreferencesKey("hatirlatma_acik")

    fun hatirlatmaAcik(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { prefs -> prefs[hatirlatmaKey] ?: false }
    }

    suspend fun hatirlatmaAyarla(context: Context, acik: Boolean) {
        context.dataStore.edit { prefs -> prefs[hatirlatmaKey] = acik }
    }

    // Geçmişten bir kaydı SİL (indexe göre)
    suspend fun removeFromHistory(context: Context, index: Int) {
        context.dataStore.edit { prefs ->
            val raw = prefs[gecmisKey] ?: "[]"
            val jsonArray = JSONArray(raw)
            if (index in 0 until jsonArray.length()) {
                jsonArray.remove(index)
                prefs[gecmisKey] = jsonArray.toString()
            }
        }
    }
}
