package com.knowtomigrate.app.updater

import org.json.JSONObject
import java.util.Locale

data class AndroidUpdateManifest(
    val product: String = "KnowToMigrate",
    val platform: String = "android",
    val channel: String = "stable",
    val versionCode: Int,
    val versionName: String,
    val minimumSupportedVersionCode: Int = 10000,
    val apkUrl: String,
    val sizeBytes: Long,
    val sha256: String,
    val releaseDate: String = "",
    val mandatory: Boolean = false,
    val title: String = "",
    val releaseNotes: List<String> = emptyList()
) {
    val formattedSize: String
        get() = when {
            sizeBytes >= 1_000_000_000 -> String.format(Locale.US, "%.1f GB", sizeBytes / 1_000_000_000.0)
            sizeBytes >= 1_000_000 -> String.format(Locale.US, "%.1f MB", sizeBytes / 1_000_000.0)
            sizeBytes >= 1_000 -> String.format(Locale.US, "%.1f KB", sizeBytes / 1_000.0)
            else -> "$sizeBytes B"
        }

    companion object {
        fun fromJson(jsonStr: String): AndroidUpdateManifest? {
            return try {
                val clean = jsonStr.trim().removePrefix("\uFEFF").trim()
                val json = JSONObject(clean)
                // Case 1: Standalone android stable.json format
                if (json.has("platform") && json.optString("platform") == "android") {
                    val notesList = mutableListOf<String>()
                    val notesArr = json.optJSONArray("releaseNotes")
                    if (notesArr != null) {
                        for (i in 0 until notesArr.length()) {
                            notesList.add(notesArr.optString(i))
                        }
                    }
                    AndroidUpdateManifest(
                        product = json.optString("product", "KnowToMigrate"),
                        platform = "android",
                        channel = json.optString("channel", "stable"),
                        versionCode = json.optInt("versionCode", 0),
                        versionName = json.optString("versionName", ""),
                        minimumSupportedVersionCode = json.optInt("minimumSupportedVersionCode", 10000),
                        apkUrl = json.optString("apkUrl", ""),
                        sizeBytes = json.optLong("sizeBytes", 0L),
                        sha256 = json.optString("sha256", ""),
                        releaseDate = json.optString("releaseDate", ""),
                        mandatory = json.optBoolean("mandatory", false),
                        title = json.optString("title", "KnowToMigrate Update"),
                        releaseNotes = notesList
                    )
                }
                // Case 2: Unified update-manifest.json with android sub-object
                else if (json.has("android")) {
                    val androidObj = json.getJSONObject("android")
                    val notesList = mutableListOf<String>()
                    val notesArr = json.optJSONArray("releaseNotes")
                    if (notesArr != null) {
                        for (i in 0 until notesArr.length()) {
                            notesList.add(notesArr.optString(i))
                        }
                    }
                    AndroidUpdateManifest(
                        product = json.optString("product", "KnowToMigrate"),
                        platform = "android",
                        channel = json.optString("channel", "stable"),
                        versionCode = androidObj.optInt("versionCode", 0),
                        versionName = androidObj.optString("versionName", json.optString("version", "")),
                        minimumSupportedVersionCode = 10000,
                        apkUrl = androidObj.optString("url", ""),
                        sizeBytes = androidObj.optLong("size", 0L),
                        sha256 = androidObj.optString("sha256", ""),
                        releaseDate = json.optString("publishedAt", ""),
                        mandatory = json.optBoolean("mandatory", false),
                        title = "KnowToMigrate " + androidObj.optString("versionName", json.optString("version", "")),
                        releaseNotes = notesList
                    )
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }
}
