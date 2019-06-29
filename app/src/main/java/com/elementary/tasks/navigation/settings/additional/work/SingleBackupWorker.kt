package com.elementary.tasks.navigation.settings.additional.work

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.elementary.tasks.core.cloud.storages.Dropbox
import com.elementary.tasks.core.cloud.FileConfig
import com.elementary.tasks.core.cloud.storages.GDrive
import com.elementary.tasks.core.data.AppDb
import com.elementary.tasks.core.utils.Constants
import com.elementary.tasks.core.utils.MemoryUtil
import com.elementary.tasks.core.utils.launchDefault
import com.google.gson.Gson
import java.io.File
import java.io.IOException

class SingleBackupWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val uuId = inputData.getString(Constants.INTENT_ID) ?: ""
        if (uuId.isNotEmpty()) {
            launchDefault {
                val db = AppDb.getAppDatabase(applicationContext)
                val smsTemplate = db.smsTemplatesDao().getByKey(uuId)
                if (smsTemplate != null) {
                    cacheFiles(uuId + FileConfig.FILE_NAME_TEMPLATE, Gson().toJson(smsTemplate))
                }
            }
        }
        return Result.success()
    }

    private fun cacheFiles(fileName: String, data: String) {
        val dir = MemoryUtil.templatesDir
        if (dir != null) {
            try {
                MemoryUtil.writeFile(File(dir, fileName), data)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        Dropbox().uploadTemplateByFileName(fileName)
        try {
            GDrive.getInstance(applicationContext)?.saveTemplateToDrive(File(dir, fileName).toString())
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}