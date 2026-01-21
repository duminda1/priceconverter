package com.pocketcurrency.data.repository

import android.content.SharedPreferences

class FakeSharedPreferences(
    private val storage: MutableMap<String, Any?> = mutableMapOf()
) : SharedPreferences {
    override fun getAll(): MutableMap<String, *> = storage.toMutableMap()

    override fun getString(key: String?, defValue: String?): String? {
        return storage[key] as? String ?: defValue
    }

    override fun getStringSet(
        key: String?,
        defValues: MutableSet<String>?
    ): MutableSet<String>? {
        @Suppress("UNCHECKED_CAST")
        val value = storage[key] as? Set<String>
        return value?.toMutableSet() ?: defValues
    }

    override fun getInt(key: String?, defValue: Int): Int {
        return storage[key] as? Int ?: defValue
    }

    override fun getLong(key: String?, defValue: Long): Long {
        return storage[key] as? Long ?: defValue
    }

    override fun getFloat(key: String?, defValue: Float): Float {
        return storage[key] as? Float ?: defValue
    }

    override fun getBoolean(key: String?, defValue: Boolean): Boolean {
        return storage[key] as? Boolean ?: defValue
    }

    override fun contains(key: String?): Boolean {
        return storage.containsKey(key)
    }

    override fun edit(): SharedPreferences.Editor {
        return EditorImpl()
    }

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?
    ) = Unit

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?
    ) = Unit

    private inner class EditorImpl : SharedPreferences.Editor {
        private val updates = mutableMapOf<String, Any?>()
        private val removals = mutableSetOf<String>()
        private var clearAll = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor = apply {
            updates[key.orEmpty()] = value
        }

        override fun putStringSet(
            key: String?,
            values: MutableSet<String>?
        ): SharedPreferences.Editor = apply {
            updates[key.orEmpty()] = values?.toSet()
        }

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor = apply {
            updates[key.orEmpty()] = value
        }

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor = apply {
            updates[key.orEmpty()] = value
        }

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = apply {
            updates[key.orEmpty()] = value
        }

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = apply {
            updates[key.orEmpty()] = value
        }

        override fun remove(key: String?): SharedPreferences.Editor = apply {
            removals.add(key.orEmpty())
        }

        override fun clear(): SharedPreferences.Editor = apply {
            clearAll = true
        }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            if (clearAll) {
                storage.clear()
            }
            removals.forEach { storage.remove(it) }
            updates.forEach { (key, value) ->
                if (value == null) {
                    storage.remove(key)
                } else {
                    storage[key] = value
                }
            }
        }
    }
}
