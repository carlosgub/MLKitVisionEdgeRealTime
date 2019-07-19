package com.carlosgub.mlkitrealtime

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.carlosgub.mlkitrealtime.utils.toBitmap
import com.google.firebase.ml.common.FirebaseMLException
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread


private const val REQUEST_CODE_PERMISSIONS = 10

// This is an array of all the permission specified in the manifest
private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

class MainActivity : AppCompatActivity(), LifecycleOwner,ImageClassifier.Listener {

    private var classifier: ImageClassifier? = null
    private var bitmap: Bitmap? = null
    private var orientation=0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Request camera permissions
        if (allPermissionsGranted()) {
            tv.post { startCamera() }
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        tv.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateTransform()
        }

        btClearImage.setOnClickListener {
            nextImage()
            btClearImage.visibility=View.GONE
            iv.setImageBitmap(null)
        }
    }

    override fun onPause() {
        super.onPause()
        System.exit(0)
    }


    private fun startCamera() {
        // Create configuration object for the viewfinder use case
        val imageAnalysisConfig = ImageAnalysisConfig.Builder()
            .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
            //.setTargetResolution(Size(1280, 720))
            .build()
        val imageAnalysis = ImageAnalysis(imageAnalysisConfig)

        imageAnalysis.setAnalyzer { image: ImageProxy, orientation: Int ->
            if(bitmap==null){
                this.orientation = orientation
                bitmap = image.toBitmap()
                try {
                    classifier = ImageClassifier(this)
                } catch (e: FirebaseMLException) {
                    Log.d(":)",e.message.toString())
                }
                classifier?.classifyFrame(bitmap!!)

            }else{
                bitmap = image.toBitmap()
            }
        }

        val previewConfig = PreviewConfig.Builder()
            .build()
        val preview = Preview(previewConfig)

        // Every time the viewfinder is updated, recompute layout
        preview.setOnPreviewOutputUpdateListener {
            val parent = tv.parent as ViewGroup
            parent.removeView(tv)
            parent.addView(tv, 0)

            tv.surfaceTexture = it.surfaceTexture
            updateTransform()
        }

        CameraX.bindToLifecycle(this, imageAnalysis, preview)
    }

    override fun onDestroy() {
        classifier?.close()
        super.onDestroy()
    }

    private fun updateTransform() {
        val matrix = Matrix()

        // Compute the center of the view finder
        val centerX = tv.width / 2f
        val centerY = tv.height / 2f

        // Correct preview output to account for display rotation
        val rotationDegrees = when(tv.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }
        matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)

        // Finally, apply transformations to our TextureView
        tv.setTransform(matrix)
    }

    private fun updateBitmapOrientation(bitmap: Bitmap, orientation:Int):Bitmap{
        val matrix = Matrix()

        matrix.postRotate(orientation.toFloat())
        return Bitmap.createBitmap(bitmap,0,0,bitmap.width,bitmap.height,matrix,true)
    }


    /**
     * Process result from permission request dialog box, has the request
     * been granted? If yes, start Camera. Otherwise display a toast
     */
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                tv.post { startCamera() }
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    /**
     * Check if all permission specified in the manifest have been granted
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    /*private fun getBitmap(): Bitmap {
        runOnUiThread {
            updateBitmapOrientation(imageProxy!!.toBitmap(),orientation)
        }
    }*/

    override fun onError(exception: Exception) {
        //Mostrar error
        Log.d(":)",exception.message.toString())
    }

    override fun onSucess(bitmap: Bitmap) {
        iv.setImageBitmap(tv.bitmap)
        btClearImage.visibility= View.VISIBLE
    }

    override fun nextImage() {
        classifier?.classifyFrame(bitmap!!)
    }

}
