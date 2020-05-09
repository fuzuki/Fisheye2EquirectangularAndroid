package net.hiruandon.fisheye2vrequirectangular

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.media.ExifInterface
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

const val REQUEST_IMAGE_CAPTURE = 1
const val REQUEST_IMAGE_OPEN = 2

class Fisheye2EquirectangularActivity : AppCompatActivity() {

    private var canConvertFlg = false
    private var imageView: ImageView? = null
    private var fisheyeBmp: Bitmap? = null

    private var fisheyeTmpFile: File? = null
    private var fisheyeUri: Uri? = null
    private var currentPhotoPath: String = ""
    private var progressBar: ProgressBar? = null

    //private var exif: ExifInterface? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fisheye2_equirectangular)

        val cameraButton = findViewById<Button>(R.id.camera_button)
        val imageButton = findViewById<Button>(R.id.image_button)
        val convertButton = findViewById<Button>(R.id.convert_button)
        imageView = findViewById(R.id.image_view)
        progressBar = findViewById(R.id.progressBar)
        progressBar?.visibility = ProgressBar.INVISIBLE
        cameraButton.setOnClickListener {
            capturePhoto()
        }

        imageButton.setOnClickListener {
            selectImage()
        }

        convertButton.setOnClickListener {
            if(canConvertFlg && fisheyeBmp != null){
                progressBar?.visibility = ProgressBar.VISIBLE
                convertImage()
            }
        }
    }

    private fun capturePhoto() {
        /*
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, null)
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
        }
         */
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Create the File where the photo should go
                fisheyeTmpFile = try {
                    createImageFile()
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    null
                }
                // Continue only if the File was successfully created
                fisheyeTmpFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "net.hiruandon.fisheye2vrequirectangular",
                        it
                    )
                    fisheyeUri = photoURI
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            }
        }
    }

    private fun selectImage() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        // Only the system receives the ACTION_OPEN_DOCUMENT, so no need to test.
        startActivityForResult(intent, REQUEST_IMAGE_OPEN)
    }

    private fun convertImage(){
        //Toast.makeText(this,"test", Toast.LENGTH_SHORT).show()
        val f = Fisheye2Equirectangular()
        val equiImg = f.fisheye2equirectangular(fisheyeBmp as Bitmap,180F)
        imageView?.setImageBitmap(equiImg)//TODO スレッドで処理すること
        // 保存用の画像
        val saveImg = Bitmap.createBitmap(equiImg.width * 2, equiImg.height,equiImg.config)
        val canvas = Canvas(saveImg)
        canvas.drawColor(Color.BLACK)
        canvas.drawBitmap(equiImg,equiImg.width/2F,0F,null)
        // 画像保存処理
        // TODO
        //MediaStore.Images.Media.getContentUri(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        val outFile = createEquirectangularImageFile()
        val outstream = FileOutputStream(outFile)
        saveImg.compress(Bitmap.CompressFormat.JPEG,100,outstream)
        outstream.close()

        val exif = ExifInterface(outFile.absolutePath)

        //本来はXMPタグを編集すべきだが、仮に対応
        exif.setAttribute(ExifInterface.TAG_MAKE,"RICOH")
        exif.setAttribute(ExifInterface.TAG_MODEL,"RICOH THETA S")
        exif.saveAttributes()

        galleryAddPic()
        canConvertFlg = false
        progressBar?.visibility = ProgressBar.INVISIBLE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK){
            if(requestCode == REQUEST_IMAGE_CAPTURE){
                // TODO
                //fisheyeBmp = data?.extras?.get("data") as Bitmap
                val pfDescriptor = getContentResolver().openFileDescriptor(fisheyeUri as Uri, "r")
                fisheyeBmp = BitmapFactory.decodeFileDescriptor(pfDescriptor?.fileDescriptor)

                fisheyeUri = null
                fisheyeTmpFile?.delete()
            }else if(requestCode == REQUEST_IMAGE_OPEN){
                // TODO
                val pfDescriptor = getContentResolver().openFileDescriptor(data?.data as Uri, "r")
                fisheyeBmp = BitmapFactory.decodeFileDescriptor(pfDescriptor?.fileDescriptor)
                pfDescriptor?.close()
            }
            imageView?.setImageBitmap(fisheyeBmp)

            canConvertFlg = true
        }
    }

    private fun galleryAddPic() {
        Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).also { mediaScanIntent ->
            val f = File(currentPhotoPath)
            mediaScanIntent.data = Uri.fromFile(f)
            sendBroadcast(mediaScanIntent)
        }
    }
    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    @Throws(IOException::class)
    private fun createEquirectangularImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        // getExternalStoragePublicDirectory は deprecated
        val storageDir: File? = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        //val storageDir = File(MediaStore.Images.Media.EXTERNAL_CONTENT_URI.path)
        //val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "VR_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }
}
