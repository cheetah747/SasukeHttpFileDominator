package com.sibyl.httpfiledominator.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sibyl.httpfiledominator.mainactivity.view.MainActivity

/**
 * @author Sasuke on 2020/6/15.
 * 跳板Activity
 * 如果直接从通知点击，然后直接跳转MainActivity的话，会造成后面的动画全都变成那种（不知道怎么说）的切换动画
 * 而如果点击通知先跳转TempActivity，再从这里跳转到MainActivity，就不会影响到后面的界面切换动画
 */
class TempActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}