package com.sibyl.httpfiledominator.mainactivity.repo

import android.content.Intent
import android.content.res.AssetManager
import android.net.Uri
import android.os.Looper
import android.os.Parcelable
import android.util.Log
import com.sibyl.httpfiledominator.MyApp
import com.sibyl.httpfiledominator.MyHttpServer
import com.sibyl.httpfiledominator.UriInterpretation
import com.sibyl.httpfiledominator.utils.ClipServer
import com.sibyl.httpfiledominator.utils.ClipboardUtil
import com.sibyl.httpfiledominator.utils.JsonDominator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.lang.StringBuilder
import java.util.*

/**
 * @author HUANGSHI-PC on 2020-03-06 0006.
 */
class MainRepo {

    /**
     * 以下是从SendFileActivity里拷过来的。用来接收直接从外面分享进来的文件，
     * 原SendFileActivity已废弃
     */
    suspend fun getIntentFileUris(dataIntent: Intent): MutableList<UriInterpretation>? = withContext(Dispatchers.IO) {
            val theUris = mutableListOf<UriInterpretation>()
            if (Intent.ACTION_SEND_MULTIPLE == dataIntent.action) {
                dataIntent.getParcelableArrayListExtra<Parcelable>(Intent.EXTRA_STREAM)?.forEach {
                    it?.let { theUris.add(UriInterpretation(it as Uri, MyApp.instance?.contentResolver)) }
                }
                return@withContext theUris
            }
            val extras = dataIntent.extras
            if (extras == null) {//直接手动进的主页，不会存在传intent数据的情况
                throw Exception()
            }
            var myUri: Uri? = extras[Intent.EXTRA_STREAM] as Uri
            if (myUri == null) {
                val tempString = extras[Intent.EXTRA_TEXT] as String?
                if (tempString == null) {
                    throw Exception("Error obtaining the file path")
                }
                myUri = Uri.parse(tempString)
                if (myUri == null) {
                    throw Exception("Error obtaining the file path")
                }
            }
            theUris.add(UriInterpretation(myUri, MyApp.instance?.getContentResolver()))
            theUris
    }

    /**处理onActivityResult()里的新数据*/
    suspend fun getActivityResultUris(data: Intent?): MutableList<UriInterpretation> = withContext(Dispatchers.IO) {
        val theUris = mutableListOf<UriInterpretation>()
        val dataUri = data?.getData()
        if (dataUri != null) {
            theUris.add(UriInterpretation(dataUri, MyApp.instance?.getContentResolver()))
        } else {
            val clipData = data?.getClipData()
            for (i in 0 until (clipData?.itemCount ?: 0)) {
                val item = clipData?.getItemAt(i)
                val uri = item?.uri
                theUris.add(UriInterpretation(uri, MyApp.instance?.getContentResolver()))
            }
        }
        theUris
    }


    /**剪切板模式下的数据处理*/
    //如果用html的话，这里传入的File其实是没作用的
    suspend fun getTxtFromClipboard(/*clipboardFile: File*/) = withContext(Dispatchers.IO) {
        //剪切板内容写入到文件
//        JsonDominator.fire2Dir(ClipboardUtil.getText(MyApp.instance), clipboardFile)
        ClipServer.text = ClipboardUtil.getText(MyApp.instance)
        ClipServer.textRaw= Base64.getEncoder().encodeToString(ClipboardUtil.getText(MyApp.instance).toByteArray())

        val tempUris = ArrayList<UriInterpretation>().apply {
            add(UriInterpretation(Uri.EMPTY, ClipServer.text, MyApp.instance?.getContentResolver()))
                    /*UriInterpretation(Uri.fromFile(clipboardFile), ClipboardUtil.getText(MyApp.instance)*/
        }
        MyHttpServer.setClipboardUris(tempUris)
    }

    /**写入到剪切板*/
    suspend fun write2Clipboard(newText: String) = withContext(Dispatchers.IO) {
        ClipboardUtil.copyText(MyApp.instance,newText)

        val tempUris = ArrayList<UriInterpretation>().apply {
            add(UriInterpretation(Uri.EMPTY, newText, MyApp.instance?.getContentResolver()))
        }
        MyHttpServer.setClipboardUris(tempUris)
    }


    /**获取index.html*/
    suspend fun getIndexHTML(assets: AssetManager): String = withContext(Dispatchers.IO) {
        val sb = StringBuilder()
        val bReader = BufferedReader(InputStreamReader(assets.open("index.html")))
        while (true){
            val str = bReader.readLine()
            if (str == null){
                break
            }
            sb.append(str +"\n")
        }
        bReader.close()
        sb.toString()
    }
}