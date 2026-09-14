package com.mmushtaq.smartreceiptscanner.core.export

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri

object ShareUtil {
    fun shareFile(context: Context, uri: Uri, mimeType: String, chooserTitle: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, chooserTitle)
        if (context !is Activity) {
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
