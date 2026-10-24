package com.wanderwildwood.kuroban.game

import android.content.SharedPreferences
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BuzzSettingTest {

    /** Just enough of SharedPreferences for one boolean, kept in a map that outlives the setting. */
    private class FakePrefs(val values: MutableMap<String, Any?> = mutableMapOf()) : SharedPreferences {
        override fun getBoolean(key: String, defValue: Boolean) = values[key] as? Boolean ?: defValue
        override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
            val pending = mutableMapOf<String, Any?>()
            override fun putBoolean(key: String, value: Boolean) = apply { pending[key] = value }
            override fun apply() { values.putAll(pending) }
            override fun commit(): Boolean { apply(); return true }
            override fun putString(key: String, value: String?) = this
            override fun putStringSet(key: String, values: MutableSet<String>?) = this
            override fun putInt(key: String, value: Int) = this
            override fun putLong(key: String, value: Long) = this
            override fun putFloat(key: String, value: Float) = this
            override fun remove(key: String) = this
            override fun clear() = this
        }
        override fun getAll(): MutableMap<String, *> = values
        override fun getString(key: String, defValue: String?) = defValue
        override fun getStringSet(key: String, defValues: MutableSet<String>?) = defValues
        override fun getInt(key: String, defValue: Int) = defValue
        override fun getLong(key: String, defValue: Long) = defValue
        override fun getFloat(key: String, defValue: Float) = defValue
        override fun contains(key: String) = values.containsKey(key)
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    }

    @Test
    fun `it is off until somebody turns it on`() {
        assertFalse(BuzzSetting(FakePrefs()).on.value)
    }

    @Test
    fun `turning it on is remembered by the next visit`() {
        val prefs = FakePrefs()

        val first = BuzzSetting(prefs)
        first.set(true)

        assertTrue(first.on.value)
        assertTrue(BuzzSetting(prefs).on.value)
    }

    @Test
    fun `turning it off again is remembered too`() {
        val prefs = FakePrefs()
        BuzzSetting(prefs).set(true)
        BuzzSetting(prefs).set(false)

        assertFalse(BuzzSetting(prefs).on.value)
    }
}
