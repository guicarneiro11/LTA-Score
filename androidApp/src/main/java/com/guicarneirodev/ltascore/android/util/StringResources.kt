package com.guicarneirodev.ltascore.android.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource

object StringResources {

    @Composable
    fun getString(resId: Int): String {
        return stringResource(resId)
    }

    @Composable
    fun getStringFormatted(resId: Int, param: Int): String {
        return stringResource(resId, param)
    }

    @Composable
    fun getStringFormatted(resId: Int, param: String): String {
        return stringResource(resId, param)
    }

    @Composable
    fun getStringFormatted(resId: Int, vararg params: Any): String {
        return stringResource(resId, *params)
    }

    @Composable
    fun getStringByName(name: String): String {
        val context = LocalContext.current
        val resId = context.resources.getIdentifier(name, "string", context.packageName)
        return if (resId != 0) stringResource(resId) else name
    }
}