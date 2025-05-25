package com.guicarneirodev.ltascore.android.util

import android.content.Context

object StringResources {
    private lateinit var appContext: Context

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    fun getString(resId: Int): String {
        if (!::appContext.isInitialized) {
            throw IllegalStateException("StringResources not initialized. Call initialize(context) first.")
        }
        return appContext.getString(resId)
    }

    fun getStringFormatted(resId: Int, vararg formatArgs: Any): String {
        if (!::appContext.isInitialized) {
            throw IllegalStateException("StringResources not initialized. Call initialize(context) first.")
        }
        return appContext.getString(resId, *formatArgs)
    }
}