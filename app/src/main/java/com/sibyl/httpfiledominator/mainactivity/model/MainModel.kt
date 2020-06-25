package com.sibyl.httpfiledominator.mainactivity.model

import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sibyl.httpfiledominator.MyHttpServer
import com.sibyl.httpfiledominator.UriInterpretation
import com.sibyl.httpfiledominator.mainactivity.repo.MainRepo
import kotlinx.coroutines.launch
import java.io.File

/**
 * @author HUANGSHI-PC on 2020-03-06 0006.
 */
class MainModel(val repo: MainRepo) : ViewModel() {
    val isLoading = MutableLiveData<Boolean>().apply { value = false }

    val snackbarMsg = MutableLiveData<String>().apply { value = "" }

    val httpServer = MutableLiveData<MyHttpServer?>()
    /**默认显示的IP地址*/
    val preferredServerUrl = ObservableField<String>()
    /**服务器IP地址集*/
    val listOfServerUris = MutableLiveData<MutableList<String>>()

    /**每次加的新数据缓存在这里*/
    val newUrisCache = MutableLiveData<List<UriInterpretation>>()

    /**是否剪切板模式*/
    val isClipboardMode = MutableLiveData<Boolean>()

    /**刷新剪切板UI*/
    val isRefreshClipboardUI = MutableLiveData<Boolean>()

    /**处理刚进页面时传入的Intent*/
    fun dealNewIntentData(intent: Intent?) = viewModelScope.launch {
//        if (isFinishing()) conti.resumeWithException(Exception(""))
        if (intent?.extras != null) isLoading.value = true
        try {
            intent?.extras?.let {
                val sharedUriList = repo.getIntentFileUris(intent)
                dealWithNewUris(sharedUriList)
            }
        } catch (e: Exception) {
            snackbarMsg.value = e.message
        }finally {
            isLoading.value = false
        }
    }

    /**处理ActivityResult传入的Intent*/
    fun dealActivityResultIntent(intent: Intent?) = viewModelScope.launch {
        if (intent != null) isLoading.value = true
        try {
            val newUriList = repo.getActivityResultUris(intent)
            dealWithNewUris(newUriList)
        } catch (e: Exception) {
            snackbarMsg.value = e.message
        }finally {
            isLoading.value = false
        }
    }


    /**对新uri的处理*/
    fun dealWithNewUris(newUriList: MutableList<UriInterpretation>?) = viewModelScope.launch {
        //先过滤一波已经添加过的
        newUriList?.removeAll(MyHttpServer.getNormalUris())
        if (newUriList.isNullOrEmpty()) return@launch
        //如果没有建立起http服务时==========
        if (httpServer.value == null){
            httpServer.value = MyHttpServer(1120)
        }
        MyHttpServer.getNormalUris().addAll(newUriList)
        //显示到UI
        newUrisCache.value = newUriList
        MyHttpServer.changeUrisByMode(false)
        //切换模式
        isClipboardMode.value = false
    }

    /**
     * 处理剪切板情况下的数据
     */
    fun createClipDataRefresh(clipboardFile: File) = viewModelScope.launch {
        isLoading.value = true
        try{
            repo.writeClipboard2File(clipboardFile)
            if (httpServer.value == null){
                httpServer.value = MyHttpServer(1120)
            }
            isRefreshClipboardUI.value = true
        }catch (e: Exception){
            snackbarMsg.value = e.message
        }finally {
            isLoading.value = false
        }
    }
}