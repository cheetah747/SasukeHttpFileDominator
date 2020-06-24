package com.sibyl.httpfiledominator.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.sibyl.httpfiledominator.R

/**
 * @author Sasuke on 2020/6/23.
 */
open class BaseActivity: AppCompatActivity() {
    companion object{
        val HANDLER_CONNECTION_START = 42
        val HANDLER_CONNECTION_END = 4242
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    fun initUIAfter(toolbar: Toolbar){
        window.navigationBarColor = resources.getColor(R.color.main_activity_background_color, null)
        //设置ActionBar
        setSupportActionBar(toolbar.apply {
            setTitle(getString(R.string.app_name))
            setTitleTextColor(getResources().getColor(R.color.light_blue, null))
        })
    }
}