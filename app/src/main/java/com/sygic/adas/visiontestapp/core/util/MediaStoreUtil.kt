package com.sygic.adas.visiontestapp.core.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object MediaStoreUtil {

    private val collection =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        else
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI


    suspend fun exportVideos(context: Context, videos: List<File>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            exportVideosQ(context, videos)
        else
            exportVideosLegacy(context, videos)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun exportVideosQ(context: Context, videos: List<File>) {
        withContext(Dispatchers.IO) {
            videos.forEach { videoFile ->
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, videoFile.name)
                    put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/dashcam")
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
                val resolver = context.applicationContext.contentResolver
                val uri = resolver.insert(collection, values)
                uri?.let {
                    resolver.openOutputStream(it)?.use { out ->
                        videoFile.inputStream().use { ins ->
                            val buffer = ByteArray(4096)
                            var read = ins.read(buffer)
                            while (read != -1) {
                                out.write(buffer, 0, read)
                                read = ins.read(buffer)
                            }
                            out.flush()
                        }
                    }
                    values.clear()
                    values.put(MediaStore.Video.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }
            }
        }
    }

    private suspend fun exportVideosLegacy(context: Context, videos: List<File>) {
        withContext(Dispatchers.IO) {
            val newMediaFiles = mutableListOf<File>()
            videos.forEach { videoFile ->
                val targetFile = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                    videoFile.name
                )
                if(videoFile.renameTo(targetFile)) {
                    newMediaFiles.add(targetFile)
                }
            }
            MediaScannerConnection.scanFile(
                context,
                newMediaFiles.map { it.absolutePath }.toTypedArray(),
                newMediaFiles.map { "video/mp4" }.toTypedArray(),
                null
            )
        }
    }
}