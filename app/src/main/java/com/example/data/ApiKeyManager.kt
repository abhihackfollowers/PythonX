package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig

class ApiKeyManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("pythonx_secure_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_GEMINI_API = "custom_gemini_api_key"
        private const val KEY_FONT_SIZE = "editor_font_size"
        private const val KEY_AUTO_BRACKET = "auto_close_brackets"
        private const val KEY_AUTO_INDENT = "auto_indent_enabled"
    }

    fun getGeminiApiKey(): String {
        val userSavedKey = prefs.getString(KEY_GEMINI_API, "")?.trim() ?: ""
        if (userSavedKey.isNotBlank()) {
            return userSavedKey
        }
        val buildKey = BuildConfig.GEMINI_API_KEY.trim()
        return if (buildKey != "MY_GEMINI_API_KEY") buildKey else ""
    }

    fun setGeminiApiKey(key: String) {
        prefs.edit().putString(KEY_GEMINI_API, key.trim()).apply()
    }

    fun clearGeminiApiKey() {
        prefs.edit().remove(KEY_GEMINI_API).apply()
    }

    fun getFontSize(): Float {
        return prefs.getFloat(KEY_FONT_SIZE, 14f)
    }

    fun setFontSize(size: Float) {
        prefs.edit().putFloat(KEY_FONT_SIZE, size).apply()
    }

    fun isAutoBracketEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_BRACKET, true)
    }

    fun setAutoBracketEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_BRACKET, enabled).apply()
    }

    fun isAutoIndentEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_INDENT, true)
    }

    fun setAutoIndentEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_INDENT, enabled).apply()
    }
}
