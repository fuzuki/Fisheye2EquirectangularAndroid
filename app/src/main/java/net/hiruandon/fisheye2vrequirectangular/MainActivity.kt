package net.hiruandon.fisheye2vrequirectangular

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

/**
 * 起動画面
 * 許可を要請して、許可が得られない場合には終了する
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        f2eActivity()
    }

    // IntentでFisheye2EquirectangularActivity開始
    private fun f2eActivity() {
        val intent = Intent(application, Fisheye2EquirectangularActivity::class.java)
        startActivity(intent)
        finish()
    }
}
