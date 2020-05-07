package net.hiruandon.fisheye2vrequirectangular

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.Toast

const val REQUEST_IMAGE_CAPTURE = 1
const val REQUEST_IMAGE_OPEN = 2

class Fisheye2EquirectangularActivity : AppCompatActivity() {

    private var canConvertFlg = false;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fisheye2_equirectangular)

        val cameraButton = findViewById<Button>(R.id.camera_button)
        val imageButton = findViewById<Button>(R.id.image_button)
        val convertButton = findViewById<Button>(R.id.convert_button)

        cameraButton.setOnClickListener {
            capturePhoto()
        }

        imageButton.setOnClickListener {
            selectImage()
        }

        convertButton.setOnClickListener {
            if(canConvertFlg){
                convertImage()
            }
        }
    }

    fun capturePhoto() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
        }
    }

    fun selectImage() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        // Only the system receives the ACTION_OPEN_DOCUMENT, so no need to test.
        startActivityForResult(intent, REQUEST_IMAGE_OPEN)
    }

    fun convertImage(){
        Toast.makeText(this,"test", Toast.LENGTH_SHORT).show()
        canConvertFlg = false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK){
            if(requestCode == REQUEST_IMAGE_CAPTURE){
                // TODO
            }else if(requestCode == REQUEST_IMAGE_OPEN){
                // TODO
            }

            canConvertFlg = true
        }
    }
}
