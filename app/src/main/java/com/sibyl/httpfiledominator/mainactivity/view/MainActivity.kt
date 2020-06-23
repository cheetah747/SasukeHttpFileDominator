package com.sibyl.httpfiledominator.mainactivity.view

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import com.sibyl.httpfiledominator.R
import com.sibyl.httpfiledominator.databinding.ActivityMainBinding
import com.sibyl.httpfiledominator.mainactivity.model.MainModel
import com.sibyl.httpfiledominator.mainactivity.repo.MainModelFactory
import com.sibyl.httpfiledominator.mainactivity.repo.MainRepo

class MainActivity : AppCompatActivity() {

    val binding by lazy { DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main) }

    val loginModel by lazy{ ViewModelProviders.of(this, MainModelFactory(MainRepo())).get(MainModel::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setBackgroundDrawable(null)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        super.onCreate(savedInstanceState)
        bind()

    }

    fun bind(){
        binding.apply {

        }
    }
}
