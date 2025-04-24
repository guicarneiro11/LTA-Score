package com.guicarneirodev.ltascore.android.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

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

    @Composable
    fun getStringResource(resId: Int): String {
        return stringResource(resId)
    }

    @Composable
    fun getStringResourceFormatted(resId: Int, vararg formatArgs: Any): String {
        return stringResource(resId, *formatArgs)
    }
}