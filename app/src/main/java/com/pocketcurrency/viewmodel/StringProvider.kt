package com.pocketcurrency.viewmodel

import android.app.Application
import androidx.annotation.StringRes
import javax.inject.Inject

interface StringProvider {
    fun get(@StringRes resId: Int, vararg formatArgs: Any): String
}

class ResourceStringProvider @Inject constructor(
    private val application: Application
) : StringProvider {
    override fun get(@StringRes resId: Int, vararg formatArgs: Any): String {
        return if (formatArgs.isEmpty()) {
            application.getString(resId)
        } else {
            application.getString(resId, *formatArgs)
        }
    }
}
