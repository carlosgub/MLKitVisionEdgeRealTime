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
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler
import com.google.firebase.ml.vision.label.FirebaseVisionOnDeviceAutoMLImageLabelerOptions
import java.io.IOException
import java.util.*

//Clasificar imagenes con ML Kit
class ImageClassifier
@Throws(FirebaseMLException::class)
internal constructor(private val listener:Listener) {

  /** MLKit AutoML Image Classifier  */
  private val labeler: FirebaseVisionImageLabeler?

  init {
    FirebaseModelManager.getInstance()
      .registerLocalModel(
        FirebaseLocalModel.Builder(LOCAL_MODEL_NAME)
          .setAssetFilePath(LOCAL_MODEL_PATH)
          .build()
      )

    val options = FirebaseVisionOnDeviceAutoMLImageLabelerOptions.Builder()
      .setConfidenceThreshold(CONFIDENCE_THRESHOLD)
      .setLocalModelName(LOCAL_MODEL_NAME)
      .build()

    labeler = FirebaseVision.getInstance().getOnDeviceAutoMLImageLabeler(options)
  }

  interface Listener{
    fun onError(exception: Exception)
    fun onSucess(bitmap: Bitmap)
    fun nextImage()
  }

  //Clasificador
  internal fun classifyFrame(bitmap: Bitmap) {
    //Verificar que no sea nulo
    if (labeler == null) {
      Log.e(TAG, "Image classifier has not been initialized; Skipped.")
      val e = IllegalStateException("Uninitialized Classifier.")
      listener.onError(e)
    }

    //Tomar el tiempo antes de llamar al clasificador
    //val startTime = SystemClock.uptimeMillis()
    val image = FirebaseVisionImage.fromBitmap(bitmap)

    //Procesar imagen
    labeler?.let {
      it.processImage(image).continueWith { task ->
        //Tiempo cuando termina de procesar la imagen
        //val endTime = SystemClock.uptimeMillis()

        //Resultado
        val labelProbList = task.result

        ///Verificar Resultados
        //var textToShow = "Latency: " + java.lang.Long.toString(endTime - startTime) + "ms\n"

        if (!labelProbList.isNullOrEmpty()) {
          listener.onSucess(bitmap)
          Log.d(":)",printTopKLabels(labelProbList))
        }else{
          Log.d(":)","Next Image")
          listener.nextImage()
        }
      }
    }
  }

  //Cerrar el uso de  el clasificador
  internal fun close() {
    try {
      labeler?.close()
    } catch (e: IOException) {
      Log.e(TAG, "Unable to close the labeler instance", e)
    }

  }

  //Pintar el resultado
  private val printTopKLabels: (List<FirebaseVisionImageLabel>) -> String = {
    it.joinToString(
      separator = "\n",
      limit = RESULTS_TO_SHOW
    ) { label ->
      String.format(Locale.getDefault(), "Label: %s, Confidence: %4.2f", label.text, label.confidence)
    }
  }

  companion object {

    /** Tag for the [Log].  */
    private const val TAG = "MLKitAutoMLCodelab"

    //Nombre del  dataset preparado
    private const val LOCAL_MODEL_NAME = "dataset_test"

    //Path de donde  se encuentra el  data set en la carpeta assets
    private const val LOCAL_MODEL_PATH = "automl/manifest.json"

    /** Name of the remote model in Firebase ML Kit server.  */

    //Cantidad de Resultados que puede mostrar
    private const val RESULTS_TO_SHOW = 3

    //Probabilidad minima para contarlo como valido
    private const val CONFIDENCE_THRESHOLD = 0.6f
  }
}
