/**
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.carlosgub.mlkitrealtime

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.ml.common.FirebaseMLException
import com.google.firebase.ml.common.modeldownload.FirebaseLocalModel
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler
import com.google.firebase.ml.vision.label.FirebaseVisionOnDeviceAutoMLImageLabelerOptions
import java.io.IOException
import java.util.*

//Clasificar imagenes con ML Kit
class ImageClassifier {
    private var listener: Listener? = null

    interface Listener {
        fun onSuccess(barcodeValue: String)
        fun onError(error: String?)
        fun nextImage()
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    fun getQRCodeDetails(bitmap: Bitmap) {
        val options = FirebaseVisionBarcodeDetectorOptions.Builder()
            .setBarcodeFormats(
                FirebaseVisionBarcode.FORMAT_ALL_FORMATS
            )
            .build()

        /**
         * Exiten estos formatos
         *  Code 128 (FORMAT_CODE_128)
        Code 39 (FORMAT_CODE_39)
        Code 93 (FORMAT_CODE_93)
        Codabar (FORMAT_CODABAR)
        EAN-13 (FORMAT_EAN_13)
        EAN-8 (FORMAT_EAN_8)
        ITF (FORMAT_ITF)
        UPC-A (FORMAT_UPC_A)
        UPC-E (FORMAT_UPC_E)
        QR Code (FORMAT_QR_CODE)
        PDF417 (FORMAT_PDF417)
        Aztec (FORMAT_AZTEC)
        Data Matrix (FORMAT_DATA_MATRIX)
         */
        val detector = FirebaseVision.getInstance().getVisionBarcodeDetector(options)
        val image = FirebaseVisionImage.fromBitmap(bitmap)
        detector.detectInImage(image)
            .addOnSuccessListener {
                if (it.isNotEmpty()) {
                    for (firebaseBarcode in it) {
                        listener?.onSuccess(
                            firebaseBarcode.displayValue ?: ""
                        )  //Display contents inside the barcode
                    }
                } else {
                    listener?.nextImage()
                }

            }
            .addOnFailureListener {
                it.printStackTrace()
                listener?.onError(it.message)
            }
    }
}



