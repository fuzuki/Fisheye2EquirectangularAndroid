package net.hiruandon.fisheye2vrequirectangular

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat

/**
 * 起動画面
 * 許可を要請して、許可が得られない場合には終了する
 */
class MainActivity : AppCompatActivity() {

    private val REQUEST_PERMISSION = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermission()
    }

    // 位置情報許可の確認
    private fun checkPermission() {
        // 既に許可している
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            f2eActivity()
        } else {
            requestStragePermission()
        }// 拒否していた場合
    }

    // 許可を求める
    private fun requestStragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_PERMISSION
            )

        } else {
            val toast = Toast.makeText(
                this,
                getResources().getString(R.string.request_grant), Toast.LENGTH_SHORT
            )
            toast.show()

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_PERMISSION
            )

        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray)
    {

        if (requestCode == REQUEST_PERMISSION) {
            // 使用が許可された
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                f2eActivity()

            } else {
                // それでも拒否された時の対応
                val toast = Toast.makeText(
                    this,
                    getResources().getString(R.string.app_end), Toast.LENGTH_SHORT
                )
                toast.show()
                finish()
            }
        }
    }

    // IntentでFisheye2EquirectangularActivity開始
    private fun f2eActivity() {
        val intent = Intent(application, Fisheye2EquirectangularActivity::class.java)
        startActivity(intent)
        finish()
    }
}
