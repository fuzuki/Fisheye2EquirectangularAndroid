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
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Html
import android.text.method.LinkMovementMethod
import android.widget.*
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import androidx.preference.PreferenceManager

const val REQUEST_IMAGE_CAPTURE = 1
const val REQUEST_IMAGE_OPEN = 2

const val JPEG_QUALITY = 90

class Fisheye2EquirectangularActivity : AppCompatActivity() {

    private var canConvertFlg = false
    private var imageView: ImageView? = null
    private var fisheyeBmp: Bitmap? = null

    private var fisheyeTmpFile: File? = null
    private var fisheyeUri: Uri? = null
    private var progressBar: ProgressBar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fisheye2_equirectangular)

        val cameraButton = findViewById<Button>(R.id.camera_button)
        val imageButton = findViewById<Button>(R.id.image_button)
        val convertButton = findViewById<Button>(R.id.convert_button)
        imageView = findViewById(R.id.image_view)
        progressBar = findViewById(R.id.progressBar)
        progressBar?.visibility = ProgressBar.INVISIBLE

        val manual = findViewById<TextView>(R.id.manual)
        val m = LinkMovementMethod.getInstance()
        manual.movementMethod = m
        val manualurl = "<a href='%s'>\uD83D\uDCA1</a>".format(getResources().getString(R.string.help_url))
        manual.setText(Html.fromHtml(manualurl, Html.FROM_HTML_MODE_LEGACY))

        val x1920mode = findViewById<CheckBox>(R.id.x1920mode)
        val pref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        x1920mode.isChecked = pref.getBoolean("x1920mode",false)

        cameraButton.setOnClickListener {
            capturePhoto()
        }

        imageButton.setOnClickListener {
            selectImage()
        }

        convertButton.setOnClickListener {
            if(canConvertFlg && fisheyeBmp != null){
                convertImage()
            }else{
                val toast = Toast.makeText(
                    this,
                    "⛔", Toast.LENGTH_SHORT
                )
                toast.show()
            }
        }
    }

    override fun onStop() {
        super.onStop()

        val x1920mode = findViewById<CheckBox>(R.id.x1920mode)
        val pref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val edit = pref.edit()
        edit.putBoolean("x1920mode",x1920mode.isChecked)
        edit.apply()
    }

    private fun capturePhoto() {
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
        canConvertFlg = false
        progressBar?.visibility = ProgressBar.VISIBLE

        val x1920mode = findViewById<CheckBox>(R.id.x1920mode)
        if(x1920mode.isChecked){
            var img = fisheyeBmp as Bitmap
            var w = if(img.width > img.height) { 1920/2 } else { (img.width * (1920/2))/ img.height }
            var h = if(img.width < img.height) { 1920/2 } else { (img.height * (1920/2))/ img.width }
            fisheyeBmp = Bitmap.createScaledBitmap(img, w, h, true)
        }

        val task = ImageConvertTask()
        task.execute(fisheyeBmp)
    }

    private fun convertImageFinish(uri: Uri, width: Int){
        val pfDescriptor = getContentResolver().openFileDescriptor(uri, "r")
        val options = BitmapFactory.Options()
        val viewLen = if(imageView?.width as Int> imageView?.height as Int) imageView?.width else imageView?.height

        options.inSampleSize = 1
        while (viewLen as Int * options.inSampleSize < width){
            options.inSampleSize = options.inSampleSize * 2
        }
        if(options.inSampleSize > 1){
            options.inSampleSize = options.inSampleSize /2
        }
        val img = BitmapFactory.decodeFileDescriptor(pfDescriptor?.fileDescriptor,null,options)
        pfDescriptor?.close()
        imageView?.setImageBitmap(img)
        progressBar?.visibility = ProgressBar.INVISIBLE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK){
            if(requestCode == REQUEST_IMAGE_CAPTURE){
                val pfDescriptor = getContentResolver().openFileDescriptor(fisheyeUri as Uri, "r")
                fisheyeBmp = BitmapFactory.decodeFileDescriptor(pfDescriptor?.fileDescriptor)

                fisheyeUri = null
                fisheyeTmpFile?.delete()
            }else if(requestCode == REQUEST_IMAGE_OPEN){
                val pfDescriptor = getContentResolver().openFileDescriptor(data?.data as Uri, "r")
                fisheyeBmp = BitmapFactory.decodeFileDescriptor(pfDescriptor?.fileDescriptor)
                pfDescriptor?.close()
            }
            imageView?.setImageBitmap(fisheyeBmp)

            canConvertFlg = true
        }
    }

    private fun galleryAddPic(f:File) {
        Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).also { mediaScanIntent ->
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
        )
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
        )
    }

    inner class ImageConvertTask : AsyncTask<Bitmap, Int, Void>() {
        private var uri: Uri = Uri.EMPTY
        private var width = 1024
        override fun onPreExecute() {
            //text.setText("始めます")
            Thread.sleep(800)
        }

        override fun doInBackground(vararg param: Bitmap?): Void? {
            val f = Fisheye2Equirectangular()
            val equiImg = f.fisheye2equirectangular(fisheyeBmp as Bitmap,180F)
            //imageView?.setImageBitmap(equiImg)
            // 保存用の画像
            val saveImg = Bitmap.createBitmap(equiImg.width * 2, equiImg.height,equiImg.config)
            val canvas = Canvas(saveImg)
            canvas.drawColor(Color.BLACK)
            canvas.drawBitmap(equiImg,equiImg.width/2F,0F,null)
            // 画像保存処理
            //MediaStore.Images.Media.getContentUri(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            val outFile = createEquirectangularImageFile()
            val outstream = FileOutputStream(outFile)
            saveImg.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outstream)
            outstream.close()
            uri = Uri.fromFile(outFile)
            width = saveImg.width
            val exif = ExifInterface(outFile.absolutePath)

            //本来はXMPタグを編集すべきだが、仮に対応
            //https://www.facebook.com/notes/eric-cheng/editing-360-photos-injecting-metadata/10156930564975277/
            exif.setAttribute(ExifInterface.TAG_MAKE,"RICOH")
            exif.setAttribute(ExifInterface.TAG_MODEL,"RICOH THETA S")
            exif.saveAttributes()

            galleryAddPic(outFile)

            return null
        }

        override fun onPostExecute(result: Void?) {
            convertImageFinish(uri,width)
        }

    }
}
