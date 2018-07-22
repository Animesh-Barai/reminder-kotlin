package com.elementary.tasks.core.cloud

import android.content.Context
import android.os.Environment
import com.dropbox.core.DbxException
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.android.Auth
import com.dropbox.core.http.OkHttp3Requestor
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.WriteMode
import com.dropbox.core.v2.users.FullAccount
import com.dropbox.core.v2.users.SpaceUsage
import com.elementary.tasks.core.controller.EventControlFactory
import com.elementary.tasks.core.data.AppDb
import com.elementary.tasks.core.utils.BackupTool
import com.elementary.tasks.core.utils.LogUtil
import com.elementary.tasks.core.utils.MemoryUtil
import com.elementary.tasks.core.utils.Prefs
import okhttp3.OkHttpClient
import java.io.*

/**
 * Copyright 2016 Nazar Suhovich
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

class Dropbox(private val mContext: Context) {

    private val dbxFolder = "/Reminders/"
    private val dbxNoteFolder = "/Notes/"
    private val dbxGroupFolder = "/Groups/"
    private val dbxBirthFolder = "/Birthdays/"
    private val dbxPlacesFolder = "/Places/"
    private val dbxTemplatesFolder = "/Templates/"
    private val dbxSettingsFolder = "/Settings/"

    private var mDBApi: DbxClientV2? = null

    /**
     * Check if user has already connected to Dropbox from this application.
     *
     * @return Boolean
     */
    val isLinked: Boolean
        get() = mDBApi != null && Prefs.getInstance(mContext).dropboxToken != ""

    /**
     * Start connection to Dropbox.
     */
    fun startSession() {
        var token: String? = Prefs.getInstance(mContext).dropboxToken
        if (token == "") {
            token = Auth.getOAuth2Token()
        }
        if (token == null) {
            Prefs.getInstance(mContext).dropboxToken = ""
            return
        }
        Prefs.getInstance(mContext).dropboxToken = token
        val requestConfig = DbxRequestConfig.newBuilder("Just Reminder")
                .withHttpRequestor(OkHttp3Requestor(OkHttpClient()))
                .build()

        mDBApi = DbxClientV2(requestConfig, token)
    }

    /**
     * Holder Dropbox user name.
     *
     * @return String user name
     */
    fun userName(): String {
        var account: FullAccount? = null
        try {
            account = mDBApi!!.users().currentAccount
        } catch (e: DbxException) {
            e.printStackTrace()
        }

        return account?.name?.displayName ?: ""
    }

    /**
     * Holder user all apace on Dropbox.
     *
     * @return Long - user quota
     */
    fun userQuota(): Long {
        var account: SpaceUsage? = null
        try {
            account = mDBApi!!.users().spaceUsage
        } catch (e: DbxException) {
            LogUtil.e(TAG, "userQuota: ", e)
        }

        return account?.allocation?.individualValue?.allocated ?: 0
    }

    fun userQuotaNormal(): Long {
        var account: SpaceUsage? = null
        try {
            account = mDBApi!!.users().spaceUsage
        } catch (e: DbxException) {
            LogUtil.e(TAG, "userQuotaNormal: ", e)
        }

        return account?.used ?: 0
    }

    fun startLink() {
        Auth.startOAuth2Authentication(mContext, APP_KEY)
    }

    fun unlink(): Boolean {
        var `is` = false
        if (logOut()) {
            `is` = true
        }
        return `is`
    }

    private fun logOut(): Boolean {
        clearKeys()
        return true
    }

    private fun clearKeys() {
        Prefs.getInstance(mContext).dropboxToken = ""
        Prefs.getInstance(mContext).dropboxUid = ""
    }

    /**
     * Upload to Dropbox folder backup files from selected folder on SD Card.
     *
     * @param path name of folder to upload.
     */
    private fun upload(path: String) {
        startSession()
        if (!isLinked) {
            return
        }
        val sdPath = Environment.getExternalStorageDirectory()
        val sdPathDr = File(sdPath.toString() + "/JustReminder/" + path)
        val files = sdPathDr.listFiles()
        val fileLoc = sdPathDr.toString()
        if (files == null) {
            return
        }
        for (file in files) {
            val fileLoopName = file.name
            val tmpFile = File(fileLoc, fileLoopName)
            var fis: FileInputStream? = null
            try {
                fis = FileInputStream(tmpFile)
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }

            val folder: String = when {
                path.matches(MemoryUtil.DIR_NOTES_SD.toRegex()) -> dbxNoteFolder
                path.matches(MemoryUtil.DIR_GROUP_SD.toRegex()) -> dbxGroupFolder
                path.matches(MemoryUtil.DIR_BIRTHDAY_SD.toRegex()) -> dbxBirthFolder
                path.matches(MemoryUtil.DIR_PLACES_SD.toRegex()) -> dbxPlacesFolder
                path.matches(MemoryUtil.DIR_TEMPLATES_SD.toRegex()) -> dbxTemplatesFolder
                else -> dbxFolder
            }
            if (fis == null) return
            try {
                val filePath = folder + fileLoopName
                mDBApi!!.files().uploadBuilder(filePath)
                        .withMode(WriteMode.OVERWRITE)
                        .uploadAndFinish(fis)
            } catch (e: DbxException) {
                LogUtil.e(TAG, "Something went wrong while uploading.", e)
            } catch (e: IOException) {
                LogUtil.e(TAG, "Something went wrong while uploading.", e)
            }

        }
    }

    /**
     * Upload reminder backup files or selected file to Dropbox folder.
     *
     * @param fileName file name.
     */
    fun uploadReminderByFileName(fileName: String?) {
        val dir = MemoryUtil.remindersDir ?: return
        startSession()
        if (!isLinked) {
            return
        }
        if (fileName != null) {
            val tmpFile = File(dir.toString(), fileName)
            var fis: FileInputStream? = null
            try {
                fis = FileInputStream(tmpFile)
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }

            if (fis == null) return
            try {
                mDBApi!!.files().uploadBuilder(dbxFolder + fileName)
                        .withMode(WriteMode.OVERWRITE)
                        .uploadAndFinish(fis)
            } catch (e: DbxException) {
                LogUtil.e(TAG, "Something went wrong while uploading.", e)
            } catch (e: IOException) {
                LogUtil.e(TAG, "Something went wrong while uploading.", e)
            } catch (e: NullPointerException) {
                LogUtil.e(TAG, "Something went wrong while uploading.", e)
            }

        } else {
            upload(MemoryUtil.DIR_SD)
        }
    }

    /**
     * Upload all note backup files to Dropbox folder.
     */
    fun uploadNotes() {
        upload(MemoryUtil.DIR_NOTES_SD)
    }

    /**
     * Upload all group backup files to Dropbox folder.
     */
    fun uploadGroups() {
        upload(MemoryUtil.DIR_GROUP_SD)
    }

    /**
     * Upload all birthday backup files to Dropbox folder.
     */
    fun uploadBirthdays() {
        upload(MemoryUtil.DIR_BIRTHDAY_SD)
    }

    /**
     * Upload all places backup files to Dropbox folder.
     */
    fun uploadPlaces() {
        upload(MemoryUtil.DIR_PLACES_SD)
    }

    /**
     * Upload all templates backup files to Dropbox folder.
     */
    fun uploadTemplates() {
        upload(MemoryUtil.DIR_TEMPLATES_SD)
    }

    fun deleteFile(fileName: String) {
        when {
            fileName.endsWith(FileConfig.FILE_NAME_REMINDER) -> deleteReminder(fileName)
            fileName.endsWith(FileConfig.FILE_NAME_NOTE) -> deleteNote(fileName)
            fileName.endsWith(FileConfig.FILE_NAME_GROUP) -> deleteGroup(fileName)
            fileName.endsWith(FileConfig.FILE_NAME_BIRTHDAY) -> deleteBirthday(fileName)
            fileName.endsWith(FileConfig.FILE_NAME_PLACE) -> deletePlace(fileName)
            fileName.endsWith(FileConfig.FILE_NAME_TEMPLATE) -> deleteTemplate(fileName)
            fileName.endsWith(FileConfig.FILE_NAME_SETTINGS) -> deleteSettings(fileName)
        }
    }

    /**
     * Delete reminder backup file from Dropbox folder.
     *
     * @param name file name.
     */
    fun deleteReminder(name: String) {
        LogUtil.d(TAG, "deleteReminder: $name")
        startSession()
        if (!isLinked) {
            return
        }
        try {
            mDBApi!!.files().delete(dbxFolder + name)
        } catch (e: DbxException) {
            LogUtil.e(TAG, "deleteReminder: ", e)
        }

    }

    /**
     * Delete note backup file from Dropbox folder.
     *
     * @param name file name.
     */
    fun deleteNote(name: String) {
        startSession()
        if (!isLinked) {
            return
        }
        try {
            mDBApi!!.files().delete(dbxNoteFolder + name)
        } catch (e: DbxException) {
            LogUtil.e(TAG, "deleteNote: ", e)
        }

    }

    /**
     * Delete group backup file from Dropbox folder.
     *
     * @param name file name.
     */
    fun deleteGroup(name: String) {
        startSession()
        if (!isLinked) {
            return
        }
        try {
            mDBApi!!.files().delete(dbxGroupFolder + name)
        } catch (e: DbxException) {
            LogUtil.e(TAG, "deleteGroup: $name", e)
        }

    }

    /**
     * Delete birthday backup file from Dropbox folder.
     *
     * @param name file name
     */
    fun deleteBirthday(name: String) {
        startSession()
        if (!isLinked) {
            return
        }
        try {
            mDBApi!!.files().delete(dbxBirthFolder + name)
        } catch (e: DbxException) {
            LogUtil.e(TAG, "deleteBirthday: ", e)
        }

    }

    /**
     * Delete place backup file from Dropbox folder.
     *
     * @param name file name
     */
    fun deletePlace(name: String) {
        startSession()
        if (!isLinked) {
            return
        }
        try {
            mDBApi!!.files().delete(dbxPlacesFolder + name)
        } catch (e: DbxException) {
            LogUtil.e(TAG, "deletePlace: ", e)
        }

    }

    /**
     * Delete place backup file from Dropbox folder.
     *
     * @param name file name
     */
    fun deleteTemplate(name: String) {
        startSession()
        if (!isLinked) {
            return
        }
        try {
            mDBApi!!.files().delete(dbxTemplatesFolder + name)
        } catch (e: DbxException) {
            LogUtil.e(TAG, "deleteTemplate: ", e)
        }

    }

    /**
     * Delete settings backup file from Dropbox folder.
     *
     * @param name file name
     */
    fun deleteSettings(name: String) {
        startSession()
        if (!isLinked) {
            return
        }
        try {
            mDBApi!!.files().delete(dbxSettingsFolder + name)
        } catch (e: DbxException) {
            LogUtil.e(TAG, "deleteSettings: ", e)
        }

    }

    /**
     * Delete all folders inside application folder on Dropbox.
     */
    fun cleanFolder() {
        startSession()
        if (!isLinked) {
            return
        }
        deleteFolder(dbxNoteFolder)
        deleteFolder(dbxGroupFolder)
        deleteFolder(dbxBirthFolder)
        deleteFolder(dbxPlacesFolder)
        deleteFolder(dbxTemplatesFolder)
        deleteFolder(dbxSettingsFolder)
        deleteFolder(dbxFolder)
    }

    private fun deleteFolder(folder: String) {
        try {
            mDBApi!!.files().delete(folder)
        } catch (e: DbxException) {
            LogUtil.e(TAG, "deleteFolder: ", e)
        }

    }

    /**
     * Download on SD Card all template backup files found on Dropbox.
     */
    fun downloadTemplates(deleteFile: Boolean) {
        val dir = MemoryUtil.dropboxTemplatesDir ?: return
        startSession()
        if (!isLinked) {
            return
        }
        try {
            val result = mDBApi!!.files().listFolder(dbxTemplatesFolder) ?: return
            val dao = AppDb.getAppDatabase(mContext).smsTemplatesDao()
            val backupTool = BackupTool.getInstance()
            for (e in result.entries) {
                val fileName = e.name
                val localFile = File("$dir/$fileName")
                val cloudFile = dbxTemplatesFolder + fileName
                downloadFile(localFile, cloudFile)
                val template = backupTool.getTemplate(localFile.toString(), null)
                if (template != null) dao.insert(template)
                if (deleteFile) {
                    if (localFile.exists()) {
                        localFile.delete()
                    }
                    mDBApi!!.files().deleteV2(e.pathLower)
                }
            }
        } catch (e: DbxException) {
            LogUtil.e(TAG, "downloadTemplates: ", e)
        } catch (e: IOException) {
            LogUtil.e(TAG, "downloadTemplates: ", e)
        } catch (e: IllegalStateException) {
            LogUtil.e(TAG, "downloadTemplates: ", e)
        }

    }

    /**
     * Download on SD Card all reminder backup files found on Dropbox.
     */
    fun downloadReminders(deleteFile: Boolean) {
        val dir = MemoryUtil.dropboxRemindersDir ?: return
        startSession()
        if (!isLinked) {
            return
        }
        try {
            val result = mDBApi!!.files().listFolder(dbxFolder) ?: return
            val dao = AppDb.getAppDatabase(mContext).reminderDao()
            val backupTool = BackupTool.getInstance()
            for (e in result.entries) {
                val fileName = e.name
                val localFile = File("$dir/$fileName")
                val cloudFile = dbxFolder + fileName
                downloadFile(localFile, cloudFile)
                val reminder = backupTool.getReminder(localFile.toString(), null)
                if (reminder == null || reminder.isRemoved || !reminder.isActive) {
                    continue
                }
                dao.insert(reminder)
                val control = EventControlFactory.getController(reminder)
                if (control.canSkip()) {
                    control.next()
                } else {
                    control.start()
                }
                if (deleteFile) {
                    if (localFile.exists()) {
                        localFile.delete()
                    }
                    mDBApi!!.files().delete(e.pathLower)
                }
            }
        } catch (e: DbxException) {
            LogUtil.e(TAG, "downloadReminders: ", e)
        } catch (e: IOException) {
            LogUtil.e(TAG, "downloadReminders: ", e)
        } catch (e: IllegalStateException) {
            LogUtil.e(TAG, "downloadReminders: ", e)
        }

    }

    private fun downloadFile(localFile: File, cloudFile: String) {
        try {
            if (!localFile.exists()) {
                localFile.createNewFile()
            }
            val outputStream = FileOutputStream(localFile)
            mDBApi!!.files().download(cloudFile).download(outputStream)
        } catch (e1: DbxException) {
            LogUtil.e(TAG, "downloadFile: ", e1)
        } catch (e1: IOException) {
            LogUtil.e(TAG, "downloadFile: ", e1)
        }

    }

    /**
     * Download on SD Card all note backup files found on Dropbox.
     */
    fun downloadNotes(deleteFile: Boolean) {
        val dir = MemoryUtil.dropboxNotesDir ?: return
        startSession()
        if (!isLinked) {
            return
        }
        try {
            val result = mDBApi!!.files().listFolder(dbxNoteFolder) ?: return
            val dao = AppDb.getAppDatabase(mContext).notesDao()
            val backupTool = BackupTool.getInstance()
            for (e in result.entries) {
                val fileName = e.name
                val localFile = File("$dir/$fileName")
                val cloudFile = dbxNoteFolder + fileName
                downloadFile(localFile, cloudFile)
                val note = backupTool.getNote(localFile.toString(), null)
                if (note != null) {
                    dao.insert(note)
                }
                if (deleteFile) {
                    if (localFile.exists()) {
                        localFile.delete()
                    }
                    mDBApi!!.files().delete(e.pathLower)
                }
            }
        } catch (e: DbxException) {
            LogUtil.e(TAG, "downloadNotes: ", e)
        } catch (e: IOException) {
            LogUtil.e(TAG, "downloadNotes: ", e)
        } catch (e: IllegalStateException) {
            LogUtil.e(TAG, "downloadNotes: ", e)
        }

    }

    /**
     * Download on SD Card all group backup files found on Dropbox.
     */
    fun downloadGroups(deleteFile: Boolean) {
        val dir = MemoryUtil.dropboxGroupsDir ?: return
        startSession()
        if (!isLinked) {
            return
        }
        try {
            val result = mDBApi!!.files().listFolder(dbxGroupFolder) ?: return
            val dao = AppDb.getAppDatabase(mContext).groupDao()
            val backupTool = BackupTool.getInstance()
            for (e in result.entries) {
                val fileName = e.name
                val localFile = File("$dir/$fileName")
                val cloudFile = dbxGroupFolder + fileName
                downloadFile(localFile, cloudFile)
                val group = backupTool.getGroup(localFile.toString(), null)
                if (group != null) {
                    dao.insert(group)
                }
                if (deleteFile) {
                    if (localFile.exists()) {
                        localFile.delete()
                    }
                    mDBApi!!.files().delete(e.pathLower)
                }
            }
        } catch (e: DbxException) {
            LogUtil.e(TAG, "downloadGroups: ", e)
        } catch (e: IOException) {
            LogUtil.e(TAG, "downloadGroups: ", e)
        } catch (e: IllegalStateException) {
            LogUtil.e(TAG, "downloadGroups: ", e)
        }

    }

    /**
     * Download on SD Card all birthday backup files found on Dropbox.
     */
    fun downloadBirthdays(deleteFile: Boolean) {
        val dir = MemoryUtil.dropboxBirthdaysDir ?: return
        startSession()
        if (!isLinked) {
            return
        }
        try {
            val result = mDBApi!!.files().listFolder(dbxBirthFolder) ?: return
            val dao = AppDb.getAppDatabase(mContext).birthdaysDao()
            val backupTool = BackupTool.getInstance()
            for (e in result.entries) {
                val fileName = e.name
                val localFile = File("$dir/$fileName")
                val cloudFile = dbxBirthFolder + fileName
                downloadFile(localFile, cloudFile)
                val birthday = backupTool.getBirthday(localFile.toString(), null)
                if (birthday != null) {
                    dao.insert(birthday)
                }
                if (deleteFile) {
                    if (localFile.exists()) {
                        localFile.delete()
                    }
                    mDBApi!!.files().delete(e.pathLower)
                }
            }
        } catch (e: DbxException) {
            LogUtil.e(TAG, "downloadBirthdays: ", e)
        } catch (e: IOException) {
            LogUtil.e(TAG, "downloadBirthdays: ", e)
        } catch (e: IllegalStateException) {
            LogUtil.e(TAG, "downloadBirthdays: ", e)
        }

    }

    /**
     * Download on SD Card all places backup files found on Dropbox.
     */
    fun downloadPlaces(deleteFile: Boolean) {
        val dir = MemoryUtil.dropboxPlacesDir ?: return
        startSession()
        if (!isLinked) {
            return
        }
        try {
            val result = mDBApi!!.files().listFolder(dbxPlacesFolder) ?: return
            val dao = AppDb.getAppDatabase(mContext).placesDao()
            val backupTool = BackupTool.getInstance()
            for (e in result.entries) {
                val fileName = e.name
                val localFile = File("$dir/$fileName")
                val cloudFile = dbxPlacesFolder + fileName
                downloadFile(localFile, cloudFile)
                val place = backupTool.getPlace(localFile.toString(), null)
                if (place != null) {
                    dao.insert(place)
                }
                if (deleteFile) {
                    if (localFile.exists()) {
                        localFile.delete()
                    }
                    mDBApi!!.files().delete(e.pathLower)
                }
            }
        } catch (e: DbxException) {
            LogUtil.e(TAG, "downloadPlaces: ", e)
        } catch (e: IOException) {
            LogUtil.e(TAG, "downloadPlaces: ", e)
        } catch (e: IllegalStateException) {
            LogUtil.e(TAG, "downloadPlaces: ", e)
        }

    }

    fun uploadSettings() {
        val dir = MemoryUtil.prefsDir ?: return
        startSession()
        if (!isLinked) {
            return
        }
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (!file.toString().endsWith(FileConfig.FILE_NAME_SETTINGS)) {
                continue
            }
            var fis: FileInputStream? = null
            try {
                fis = FileInputStream(file)
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }

            if (fis == null) return
            try {
                mDBApi!!.files().uploadBuilder(dbxSettingsFolder + file.name)
                        .withMode(WriteMode.OVERWRITE)
                        .uploadAndFinish(fis)
            } catch (e: DbxException) {
                LogUtil.e(TAG, "Something went wrong while uploading.", e)
            } catch (e: IOException) {
                LogUtil.e(TAG, "Something went wrong while uploading.", e)
            }

            break
        }
    }

    fun downloadSettings() {
        val dir = MemoryUtil.prefsDir ?: return
        startSession()
        if (!isLinked) {
            return
        }
        try {
            val result = mDBApi!!.files().listFolder(dbxSettingsFolder) ?: return
            for (e in result.entries) {
                val fileName = e.name
                if (fileName.contains(FileConfig.FILE_NAME_SETTINGS)) {
                    val localFile = File("$dir/$fileName")
                    val cloudFile = dbxPlacesFolder + fileName
                    downloadFile(localFile, cloudFile)
                    Prefs.getInstance(mContext).loadPrefsFromFile()
                    break
                }
            }
        } catch (e: DbxException) {
            LogUtil.e(TAG, "downloadSettings: ", e)
        }

    }

    /**
     * Count all reminder backup files in Dropbox folder.
     *
     * @return number of found backup files.
     */
    fun countFiles(): Int {
        var count = 0
        startSession()
        if (!isLinked) {
            return 0
        }
        try {
            val result = mDBApi!!.files().listFolder("/") ?: return 0
            count = result.entries.size
        } catch (e: DbxException) {
            LogUtil.e(TAG, "countFiles: ", e)
        }

        return count
    }

    companion object {

        private const val TAG = "Dropbox"
        private const val APP_KEY = "4zi1d414h0v8sxe"
    }
}