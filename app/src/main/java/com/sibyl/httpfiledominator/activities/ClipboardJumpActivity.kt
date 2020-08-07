package com.sibyl.httpfiledominator.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.sibyl.httpfiledominator.mainactivity.view.MainActivity

/**
 * @author HUANGSHI-PC on 2020-08-06 0006.
 * 长按图标跳转剪切板模式
 */
class ClipboardJumpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, MainActivity::class.java).apply {
            putExtra("isClipboardMode",true)
        })
        finish()
    }
}