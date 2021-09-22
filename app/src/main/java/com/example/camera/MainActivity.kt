package com.example.camera

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.camera.Constants.TAG
import com.example.camera.databinding.ActivityMainBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import java.io.File
import java.lang.Exception
import java.security.acl.Permission
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.jar.Manifest

@androidx.camera.core.ExperimentalGetImage
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var imageCapture: ImageCapture? = null

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                Constants.requiredPermission,
                Constants.REQUEST_CODE_PERMISSION
            )
        }


        binding.captureBtn.setOnClickListener { takePhoto() }

        outputDirectory = getOutputDirectory()


    }


    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    private fun takePhoto() {

        val imageCapture = imageCapture ?: return

        // Create time-stamped output file to hold the image
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(
                Constants.FILE_NAME, Locale.US
            ).format(System.currentTimeMillis()) + ".jpg"
        )

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Set up image capture listener, which is triggered after photo has
        // been taken

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo capture succeeded: $savedUri"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                }
            }
        )


    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == Constants.REQUEST_CODE_PERMISSION) {
            if (allPermissionGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun startCamera() {


        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.cameraView.surfaceProvider)
                }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            imageCapture = ImageCapture.Builder()
                .build()

            //Image Analyzer Work
//            val imageAnalysis = ImageAnalysis.Builder()
//                .setTargetResolution(Size(1280, 720))
//                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//                .build()
//
//            imageAnalysis.setAnalyzer(cameraExecutor, YourImageAnalyzer())

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()


                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
                )


            } catch (ex: Exception) {
                Log.e(TAG, "Use case binding failed", ex)
            }


        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionGranted() =
        Constants.requiredPermission.all {
            ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
        }


    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }


//    private class YourImageAnalyzer : ImageAnalysis.Analyzer {
//
//        override fun analyze(imageProxy: ImageProxy) {
//            val mediaImage = imageProxy.image
//            if (mediaImage != null) {
//                val image =
//                    InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
//                // Pass image to an ML Kit Vision API
//                // ...
//                setupObjectTracking(image)
//
//            }
//        }
//
//        private fun setupObjectTracking(image: InputImage) {
//            val options = ObjectDetectorOptions.Builder()
//                .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
//                .enableClassification()
//                .build()
//
//            val objectDetector = ObjectDetection.getClient(options)
//
//            objectDetector.process(image)
//                .addOnSuccessListener { detectedObject ->
//                    Log.d("ImageDetect","Detection ${detectedObject}")
//
//                }
//                .addOnFailureListener {
//
//                }
//
//        }
//    }


}
