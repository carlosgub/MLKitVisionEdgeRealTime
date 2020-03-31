package com.carlosgub.mlkitrealtime

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.firebase.ml.common.FirebaseMLException
import kotlinx.android.synthetic.main.activity_main.*


private const val REQUEST_CODE_PERMISSIONS = 10

// This is an array of all the permission specified in the manifest
private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

class MainActivity : AppCompatActivity(), LifecycleOwner,ImageClassifier.Listener {

    private var classifier= ImageClassifier() //Clase de la clasificadora de Imagenes
    private var onPause = false //Verificar que el activity no esta en OnPause

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Verificar Permisos
        if (allPermissionsGranted()) {
            tv.post { startCamera() }
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        tv.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateTransform()
        }

        //Limpiar ImageView
        btClearImage.setOnClickListener {
            nextImage()
            btClearImage.visibility=View.GONE
            iv.setImageBitmap(null)
        }
    }

    override fun onPause() {
        super.onPause()
        onPause = true
    }

    override fun onResume() {
        super.onResume()
        onPause = false
        if (tv.bitmap != null) btClearImage.performClick()
    }


    private fun startCamera() {
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

            classifier.setListener(this)
            classifier.getQRCodeDetails(tv.bitmap)
        }

        CameraX.bindToLifecycle(this, preview)
    }

    private fun updateTransform() {
        val matrix = Matrix()

        // Compute the center of the view finder
        val centerX = tv.width.toFloat()
        val centerY = tv.height.toFloat()

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

    override fun onError(error: String?) {
        Log.d(":)",error.toString())
    }

    override fun onSuccess(barcodeValue: String) {
        Toast.makeText(this, barcodeValue, Toast.LENGTH_LONG).show()
        btClearImage.visibility = View.VISIBLE
    }

    override fun nextImage() {
        if(!onPause)classifier.getQRCodeDetails(tv.bitmap)
    }

}
