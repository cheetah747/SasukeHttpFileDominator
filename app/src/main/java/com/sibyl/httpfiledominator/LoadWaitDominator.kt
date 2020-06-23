@file:JvmName("LoadWaitDominator")

package com.sibyl.httpfiledominator

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

/**
 * @author Sasuke on 2018/3/4.
 * 加载中、暂无数据、网络错误等等状态显示的全家桶。
 *
 * 注：传入的container最好是RelativeLayout
 */

class LoadWaitDominator(val context: Context, private val container: ViewGroup?) {
    companion object {
        const val LOADING = 0//正在加载
        const val NO_DATA = 1//暂无数据
        const val NET_ERROR = 2//网络错误
        const val DIY = 4//自定义文字显示
        const val DISMISS = 5//什么都不显示，直接把转圈圈去掉
    }

    /**正在加载*/
    private val loadingView: View by lazy {
        LayoutInflater.from(context).inflate(R.layout.view_loading, container, false).apply { tag = LOADING }
    }

    /**暂无数据*/
    private val nodataView: View by lazy {
        LayoutInflater.from(context).inflate(R.layout.view_load_error, container, false).apply {
            tag = NO_DATA
            findViewById<TextView>(R.id.message).text = "暂无数据"
        }
    }

    /**网络错误*/
    private val netErrorVIew: View by lazy {
        LayoutInflater.from(context).inflate(R.layout.view_load_error, container, false).apply {
            tag = NET_ERROR
            findViewById<TextView>(R.id.message).text = "网络异常，请稍后再试"
        }
    }

    /**自定义文字*/
    private val diyView: View by lazy {
        LayoutInflater.from(context).inflate(R.layout.view_load_error, container, false).apply { tag = DIY }
    }

    /**
     * 所有View的集合
     */
    private val viewArray = arrayOf(loadingView, nodataView, netErrorVIew, diyView)

    init {
        viewArray.forEach {
            //先默认把View都添加为gone，然后再显示需要显示的
            container?.addView(it.apply { visibility = View.GONE })
        }
    }

    /**
     * 设置背景色
     */
    fun setBackgroundColor(color: Int) {
        viewArray.forEach {
            it.setBackgroundColor(color)
        }
    }

    /**
     * 切换显示内容
     * state: 传LoadViewDominator里面的那几个int静态常量标记！！！不要瞎鸡巴传。
     */
    fun show(state: Int) {
        Handler(Looper.getMainLooper()).post {
            viewArray.forEach { it.visibility = if (state != DISMISS && state == it.tag as Int) View.VISIBLE else View.GONE }
        }
    }

    /**
     * 显示自定义文字
     */
    fun show(showText: String) {
        Handler(Looper.getMainLooper()).post {
            show(DIY)
            diyView.findViewById<TextView>(R.id.message).text = showText
        }
    }

    /**
     * 隐藏所有View
     */
    fun dismissAll() {
        Handler(Looper.getMainLooper()).post {
            viewArray.forEach { it.visibility = View.GONE }
        }
    }

    /**
     * 通过 loadingView 的显隐，来判断是否在加载
     */
    fun isLoading(): Boolean {
        return loadingView.visibility == View.VISIBLE
    }
}