package com.sibyl.httpfiledominator.utils

import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.sibyl.httpfiledominator.R

/**
 * @author Sasuke on 2020/6/21.
 */
fun show(view: View,msg: String){
    Snackbar.make(view,msg,Snackbar.LENGTH_LONG)
            .apply { getView().setBackgroundResource(R.color.snackbar_background_color) }
            .show()
}