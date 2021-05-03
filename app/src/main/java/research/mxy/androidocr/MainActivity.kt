package research.mxy.androidocr

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.gson.*
import com.theartofdev.edmodo.cropper.CropImage
import pl.aprilapps.easyphotopicker.DefaultCallback
import pl.aprilapps.easyphotopicker.EasyImage
import java.io.ByteArrayOutputStream
import java.io.File


class MainActivity : AppCompatActivity() {

    private var imageFile: File? = null
    private lateinit var result: CropImage.ActivityResult
    private var isImagePicked: Boolean = false


    //To recognize text in an image, invoke the callable function, passing a JSON Cloud Vision request
    // First initialize an instance of Cloud Functions.
    private lateinit var functions: FirebaseFunctions


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        functions = FirebaseFunctions.getInstance()
        setupUI()


    }

    private fun setupUI() {
        val buttonSO = findViewById<Button>(R.id.buttonSignOut)
        buttonSO.setOnClickListener() {
            signOut()
        }

        val buttonFromGallery = findViewById<Button>(R.id.buttonFromGallery)
        buttonFromGallery.setOnClickListener() {
            EasyImage.openGallery(this, 0)
        }

        val buttonFromCamera = findViewById<Button>(R.id.buttonFromCamera)
        buttonFromCamera.setOnClickListener() {
            if (allPermissionGranted()) {
                EasyImage.openCameraForImage(this, 0)
            } else {
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
                Toast.makeText(this, "Grant camera permission in settings to continue", Toast.LENGTH_LONG)
                        .show()
            }
        }

        // Start to scan text from the picked image
        val buttonStart = findViewById<Button>(R.id.buttonStart)
        buttonStart.setOnClickListener() {

            if (isImagePicked == true) {
                // Get the image as a Bitmap object
                var bitmap: Bitmap = MediaStore.Images.Media.getBitmap(contentResolver, result.uri)

                // Scale down bitmap size
                bitmap = scaleBitmapDown(bitmap, 640)

                // Convert bitmap to base64 encoded string
                val byteArrayOutputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
                val imageBytes: ByteArray = byteArrayOutputStream.toByteArray()
                val base64encoded = Base64.encodeToString(imageBytes, Base64.NO_WRAP)


                // Create json request to cloud vision
                val request = JsonObject()
                // Add image to request
                val image = JsonObject()
                image.add("content", JsonPrimitive(base64encoded))
                request.add("image", image)

                //Add features to the request
                val feature = JsonObject()
                feature.add("type", JsonPrimitive("TEXT_DETECTION"))
                // Alternatively, for DOCUMENT_TEXT_DETECTION:
                // feature.add("type", JsonPrimitive("DOCUMENT_TEXT_DETECTION"))
                val features = JsonArray()
                features.add(feature)
                request.add("features", features)




                annotateImage(request.toString())
                        .addOnCompleteListener { task ->
                            if (!task.isSuccessful) {
                                // Task failed with an exception
                                Toast.makeText(this, "Failed to initialize OCR engine :(", Toast.LENGTH_LONG)
                                        .show()
                            } else {
                                // Task completed successfully
                                val annotation = task.result!!.asJsonArray[0].asJsonObject["fullTextAnnotation"].asJsonObject
                                System.out.format("%nComplete annotation:")
                                System.out.format("%n%s", annotation["text"].asString)

                                // Pass result to TextResultActivity and start it
                                var dataPasser = Intent(this, TextResultActivity::class.java)
                                dataPasser.putExtra("result",annotation["text"].asString)
                                startActivity(dataPasser)

                            }
                        }


            } else {
                Toast.makeText(this, "You haven't picked any images to scan", Toast.LENGTH_LONG)
                        .show()
            }
        }
    }

    //Define a method for invoking the function.
    private fun annotateImage(requestJson: String): Task<JsonElement> {
        return functions
                .getHttpsCallable("annotateImage")
                .call(requestJson)
                .continueWith { task ->
                    // This continuation runs on either success or failure, but if the task
                    // has failed then result will throw an Exception which will be
                    // propagated down.
                    val result = task.result?.data
                    JsonParser.parseString(Gson().toJson(result))
                }
    }


    // scale down the image to save on bandwidth
    private fun scaleBitmapDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height
        var resizedWidth = maxDimension
        var resizedHeight = maxDimension
        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension
            resizedWidth =
                    (resizedHeight * originalWidth.toFloat() / originalHeight.toFloat()).toInt()
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension
            resizedHeight =
                    (resizedWidth * originalHeight.toFloat() / originalWidth.toFloat()).toInt()
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension
            resizedWidth = maxDimension
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false)
    }

    private fun allPermissionGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
                baseContext, it) == PackageManager.PERMISSION_GRANTED
    }


    private fun signOut() {
        FirebaseAuth.getInstance().signOut();
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        EasyImage.handleActivityResult(
                requestCode,
                resultCode,
                data,
                this,
                object : DefaultCallback() {

                    override fun onImagesPicked(
                            imageFiles: MutableList<File>,
                            source: EasyImage.ImageSource?,
                            type: Int
                    ) {
                        CropImage.activity(Uri.fromFile(imageFiles[0])).start(this@MainActivity)
                    }

                })

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                result = CropImage.getActivityResult(data)
                imageFile = File(result.uri.path)
                loadImage(imageFile)

                isImagePicked = true

                val buttonStartDisabled = findViewById<Button>(R.id.buttonStartDisabled)
                buttonStartDisabled.isVisible = false;

                val buttonStart = findViewById<Button>(R.id.buttonStart)
                buttonStart.isVisible = true
            }
        }

    }

    private fun loadImage(imageFile: File?) {
        val imageView = findViewById<ImageView>(R.id.imageView)
        Glide.with(this)
                .load(imageFile)
                .into(imageView)

    }


    companion object {
        fun getLaunchIntent(from: Context) = Intent(from, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }

        /*private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"*/
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

    }


}