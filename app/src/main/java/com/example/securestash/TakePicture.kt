package com.example.securestash

import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import com.example.securestash.DataModels.ItemType
import com.example.securestash.Helpers.UtilityHelper
import com.example.securestash.databinding.ActivityTakePictureBinding
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class TakePicture : AppCompatActivity() {
    private val REQUEST_CAMERA_PERMISSION = 100

    private lateinit var binding: ActivityTakePictureBinding

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var itemPath: String

    private fun showPermissionExplanation() {
        AlertDialog.Builder(this)
            .setMessage("Camera Permission is required to take photos with the camera.")
            .setPositiveButton("OK") { _, _ ->
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    REQUEST_CAMERA_PERMISSION
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSettingsDialog() {
        AlertDialog.Builder(this)
            .setMessage("Camera permission is required for this feature. Please enable it in the app settings.")
            .setPositiveButton("Go to Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_CAMERA_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Permission", "Camera permission granted")
                    startCamera()
                } else {
                    Log.d("Permission", "Camera permission denied")
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                        showPermissionExplanation()
                    } else {
                        showSettingsDialog()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
        }

        binding = ActivityTakePictureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        startCamera()

        val extras = intent.extras
        if (extras != null) {
            itemPath = extras.getString("ITEM_PATH", "null")
            Log.d("TakePictureItemPath", itemPath)
        } else {
            throw Exception("Intent extras not provided.")
        }

        binding.buttonCaptureImage.setOnClickListener {
            takePhoto(object : PhotoCaptureCallback {
                override fun onPhotoCaptured(photoUri: String?) {
                    if (photoUri != null) {
                        val itemType = ItemType.IMAGE

                        val uri = Uri.parse(photoUri)
                        var imageBytes: ByteArray? = null
                        val inputStream: InputStream? = baseContext.contentResolver.openInputStream(uri)
                        inputStream?.use { stream ->
                            val outputStream = ByteArrayOutputStream()
                            val buffer = ByteArray(1024)
                            var bytesRead: Int
                            while (stream.read(buffer).also { bytesRead = it } != -1) {
                                outputStream.write(buffer, 0, bytesRead)
                            }
                            imageBytes = outputStream.toByteArray()
                        }

                        val tempFileDirectory = File(filesDir, "Temp")
                        if (!tempFileDirectory.exists()) {
                            tempFileDirectory.mkdir()
                        }

                        val tempFile = File(tempFileDirectory, UtilityHelper.getFileNameFromUri(
                            contentResolver = baseContext.contentResolver,
                            uri = uri
                        ))

                        val combinedBytes = itemType.typeBytes + imageBytes!!
                        FileOutputStream(tempFile).use { outputStream ->
                            outputStream.write(combinedBytes)
                        }

                        setResult(RESULT_OK)
                        val intent = Intent(this@TakePicture, LoadingScreen::class.java)
                        intent.putExtra("SPECIFIED_DIR", itemPath)
                        intent.putExtra("LOAD_TYPE", "ENCODE")
                        startActivity(intent)
                        finish()
                    }
                }
            })
        }

        binding.buttonBack.setOnClickListener {
            setResult(RESULT_CANCELED)
            val intent = Intent(this, FileDirectory::class.java)
            intent.putExtra("SPECIFIED_DIR", itemPath)
            startActivity(intent)
            finish()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun takePhoto(callback: PhotoCaptureCallback) {
        val imageCapture = imageCapture ?: return

        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SSTempPictures")
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
            .build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("PHOTOCAPERROR", "Photo capture failed: ${exc.message}", exc)
                    Toast.makeText(this@TakePicture, "Taking photo failed", Toast.LENGTH_SHORT).show()
                    callback.onPhotoCaptured(null)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Toast.makeText(this@TakePicture, msg, Toast.LENGTH_SHORT).show()

                    val outputFile = output.savedUri.toString()

                    callback.onPhotoCaptured(outputFile)
                }
            }
        )
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val viewFinder = binding.viewFinder

            viewFinder.post {
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(viewFinder.surfaceProvider)
                    }

                imageCapture = ImageCapture.Builder()
                    .build()

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider?.unbindAll()
                    cameraProvider?.bindToLifecycle(
                        this, cameraSelector, preview, imageCapture
                    )

                } catch (exc: Exception) {
                    Log.e(TAG, "Use case binding failed", exc)
                }
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        cameraProvider?.unbindAll()
    }

    companion object {
        private const val TAG = "TakePictureActivity"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"

    }

    interface PhotoCaptureCallback {
        fun onPhotoCaptured(photoUri: String?)
    }

}

