package com.sibyl.httpfiledominator.utils

import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.sibyl.httpfiledominator.R

/**
 * @author Sasuke on 2020/6/21.
 */
//SasukeTodo 重构完了记得把这个删掉，这是用来兼容老MainActivity的
fun show(view: View,msg: String){
    Snackbar.make(view,msg,Snackbar.LENGTH_LONG)
            .apply { getView().setBackgroundResource(R.color.snackbar_background_color) }
            .show()
}

fun showSnackbar(view: View,msg: String){
    Snackbar.make(view,msg,Snackbar.LENGTH_LONG)
            .apply { getView().setBackgroundResource(R.color.snackbar_background_color) }
            .show()
}