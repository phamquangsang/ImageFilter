package com.example.nhatpham.camerafilter.jobs

import android.net.Uri
import androidx.work.Data
import androidx.work.Worker
import com.example.nhatpham.camerafilter.utils.isExternalStorageWritable
import com.example.nhatpham.camerafilter.utils.reScanFile
import org.wysaid.nativePort.CGEFFmpegNativeLibrary
import org.wysaid.nativePort.CGENativeLibrary
import java.io.File

internal class GenerateFilteredVideoWorker : Worker() {

    override fun doWork(): Result {
        val inputPath = inputData.getString(KEY_INPUT_PATH, "")
        val config = inputData.getString(KEY_CONFIG, "")
        val outputPath = inputData.getString(KEY_OUTPUT_PATH, "")

        if(inputPath != null && !inputPath.isEmpty() &&
                config != null && !config.isEmpty() &&
                outputPath != null && !outputPath.isEmpty()) {
            val outputUri = generateFilteredVideo(inputPath, config, outputPath)
            outputData = Data.Builder().apply {
                putString(KEY_RESULT, outputUri.toString())
            }.build()
            return Result.SUCCESS
        }
        return Result.FAILURE
    }

    private fun generateFilteredVideo(inputPath: String, config: String, outputPath: String): Uri? {
        if(!isExternalStorageWritable()) return null

        val result = CGEFFmpegNativeLibrary.generateVideoWithFilter(outputPath, inputPath, config,
                1.0f, null, CGENativeLibrary.TextureBlendMode.CGE_BLEND_OVERLAY, 1.0f, false)
        return if (result) {
            Uri.fromFile(File(outputPath)).also {
                reScanFile(applicationContext, it)
            }
        } else null
    }

    companion object {
        const val KEY_RESULT = "key-result"
        private const val KEY_INPUT_PATH = "key-input-path"
        private const val KEY_CONFIG = "key-config"
        private const val KEY_OUTPUT_PATH = "key-output-path"

        @JvmStatic
        fun data(inputPath: String, config: String, outputPath: String) = Data.Builder().apply {
            putString(KEY_INPUT_PATH, inputPath)
            putString(KEY_CONFIG, config)
            putString(KEY_OUTPUT_PATH, outputPath)
        }.build()
    }
}