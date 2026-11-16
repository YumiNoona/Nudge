package com.nudge.android.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Bounded local activity only: no bodies, merchant names, account digits or references. */
object CaptureDiagnostics {
    const val KEY = "capture_activity_v1"
    private val lock = Any()
    data class Event(val at: Long, val kind: String, val source: String, val reason: String)
    fun record(context: Context, kind: String, source: String, reason: String) = synchronized(lock) {
        val prefs = context.getSharedPreferences("nudge_prefs", Context.MODE_PRIVATE)
        val rows = runCatching { JSONArray(prefs.getString(KEY, "[]")) }.getOrDefault(JSONArray())
        val next = JSONArray().put(JSONObject().put("at", System.currentTimeMillis()).put("kind", kind)
            .put("source", CaptureDuplicateMatcher.family(source)).put("reason", reason))
        for (index in 0 until minOf(rows.length(), 39)) next.put(rows.getJSONObject(index))
        prefs.edit().putString(KEY, next.toString()).apply()
    }
    fun read(context: Context): List<Event> = runCatching {
        val prefs = context.getSharedPreferences("nudge_prefs", Context.MODE_PRIVATE)
        val rows = JSONArray(prefs.getString(KEY, "[]"))
        (0 until rows.length()).map { rows.getJSONObject(it) }.map { Event(it.getLong("at"), it.getString("kind"), it.getString("source"), it.getString("reason")) }
    }.getOrDefault(emptyList())
}
