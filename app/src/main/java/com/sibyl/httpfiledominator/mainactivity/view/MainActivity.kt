package com.sibyl.httpfiledominator.mainactivity.view

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.*
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.view.get
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.hjq.permissions.OnPermission
import com.hjq.permissions.XXPermissions
import com.sibyl.httpfiledominator.LoadWaitDominator
import com.sibyl.httpfiledominator.MyHttpServer
import com.sibyl.httpfiledominator.R
import com.sibyl.httpfiledominator.UriInterpretation
import com.sibyl.httpfiledominator.activities.BaseActivity
import com.sibyl.httpfiledominator.databinding.ActivityMainBinding
import com.sibyl.httpfiledominator.mainactivity.model.MainModel
import com.sibyl.httpfiledominator.mainactivity.repo.MainModelFactory
import com.sibyl.httpfiledominator.mainactivity.repo.MainRepo
import com.sibyl.httpfiledominator.utils.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.link_layout.*
import kotlinx.android.synthetic.main.toolbar.toolbar
import java.io.File

open class MainActivity : BaseActivity() {
    companion object {
        const val REQUEST_CODE = 1024
        const val HANDLER_CONNECTION_START = 42
        const val HANDLER_CONNECTION_END = 4242
    }

    val binding by lazy { DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main) }

    val mainModel by lazy { ViewModelProviders.of(this, MainModelFactory(MainRepo())).get(MainModel::class.java) }

    val clipboardFile by lazy { File(getExternalFilesDir(null).getAbsolutePath() + File.separator + "clipboard.txt") }

    protected var loadWait: LoadWaitDominator? = null

    val notiDominator by lazy { NotiDominator(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
//        window.setBackgroundDrawable(null)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        super.onCreate(savedInstanceState)
//        if (intent.getBooleanExtra("isStopServer", false)) {
//            stopServer()
//            finish()
//        }
        bind()
        dealIntent(intent)
        setObservers()
        setListeners()
        grantPermissions() //操你妈6.0权限
        mainModel.dealNewIntentData(intent)
    }

    override fun onNewIntent(newIntent: Intent?) {
        super.onNewIntent(newIntent)
//        if (newIntent?.getBooleanExtra("isStopServer", false) ?: false) {
//            newIntent?.putExtra("isStopServer", false)
//            stopServer()
//            finish()
//        }
        dealIntent(newIntent)
        mainModel.dealNewIntentData(newIntent)
    }


    private fun dealIntent(intent: Intent?){
        if (intent?.getBooleanExtra("isStopServer", false) == true) {
//            intent?.putExtra("isStopServer", false)
            stopServer()
            finish()
        }
        if (intent?.getBooleanExtra("isClipboardMode", false) == true) {
//            intent?.putExtra("isStopServer", false)
            mainModel.isClipboardMode.value = true
        }

    }

    /**绑他妈的*/
    fun bind() {
        binding.apply {
            model = mainModel
            loadWait = LoadWaitDominator(this@MainActivity, containerLayout)

        }
        initUIAfter(toolbar)
    }

    /**观察者们*/
    fun setObservers() {
        mainModel.run {
            //loadWait状态
            isLoading.observe(this@MainActivity, Observer {
                loadWait?.show(if (it) LoadWaitDominator.LOADING else LoadWaitDominator.DISMISS)
            })
            //snackbar的显示
            snackbarMsg.observe(this@MainActivity, Observer { if (it.isNotBlank()) showSnackbar(fab, it) })

            //httpServer的初始化
            httpServer.observe(this@MainActivity, Observer {
                if (it == null) return@Observer
                listOfServerUris.value = it.listOfIpAddresses().apply {
                    preferredServerUrl.set(this[0])
                }
                //不需要了，直接用数据绑定来更新
//                runOnUiThread { link_msg.text = preferredServerUrl.get() ?: "" }
            })

            //新加数据时
            newUrisCache.observe(this@MainActivity, Observer { newUriList ->
                if (isFinishing) return@Observer
                runOnUiThread {
                    newUriList.forEach {
                        val view = LayoutInflater.from(this@MainActivity)
                                .inflate(R.layout.flex_item_files, fileNameContainer, false)
                        view.tag = it //把uriInterpretation保存到tag，到时候点击时用它来找到实时的index
                        view.findViewById<TextView>(R.id.itemNameTv).text = it.path
                        view.findViewById<ImageView>(R.id.isFolderIcon).visibility = if (it.isDirectory) View.VISIBLE else View.GONE
                        view.findViewById<ImageButton>(R.id.itemDeleteBtn).setOnClickListener {
                            val deleteIndex = MyHttpServer.getNormalUris().indexOf(view.tag as UriInterpretation)
                            if (deleteIndex != -1) {
                                fileNameContainer.removeViewAt(deleteIndex)
                                MyHttpServer.getNormalUris().removeAt(deleteIndex)
                                //如果删光了
                                if (MyHttpServer.getNormalUris().isEmpty()) {
                                    mainModel.preferredServerUrl.set("")
                                    stopServer()
                                    showSnackbar(fab, getString(R.string.files_clear_stop_server))
                                }
                            }
                        }//setOnClickListener
                        fileNameContainer.addView(view)
                        notiDominator.showNotifi()
                    }
                }
            })

            //模式切换时（剪切板 or 普通模式）
            isClipboardMode.observe(this@MainActivity, Observer {
                refreshClipModeVisibility(it)//切换组件显示状态
//                MyHttpServer.changeUrisByMode(it)//切换主url
                when(it){
                    true -> {
                        switch2ClipServer()
                        createClipDataRefresh()
                    }/*createClipDataRefresh(clipboardFile)*///剪切板模式，创建剪切板内容缓存文件，并刷新UI
                    else -> {switch2FileServer()/*if (MyHttpServer.getNormalUris().isEmpty()) stopServer() else*/ }//如果 普通模式 && 并没有添加文件，就应该把服务关掉
                }
            })

            //刷新剪切板UI显示
            isRefreshClipboardUI.observe(this@MainActivity, Observer {
                val view: View = if (clipboardContainer.childCount > 0) clipboardContainer.get(0)
                                                else LayoutInflater.from(this@MainActivity).inflate(R.layout.flex_item_clipboard, clipboardContainer, false)
                val uriInterpretation = MyHttpServer.getClipboardUris().get(0)
                val showText = if (uriInterpretation.clipboardText.isEmpty()) "（剪切板为空）" else uriInterpretation.clipboardText
                (view.findViewById<View>(R.id.itemNameTv) as TextView).apply { text = showText;requestLayout() }
                if (clipboardContainer.childCount == 0){
                    clipboardContainer.addView(view)
                }
            })
        }
    }

    /**挂监听器*/
    fun setListeners() {
        fab.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            intent.type = "*/*"
            startActivityForResult(intent, REQUEST_CODE)
        }

        clipboardBtn.setOnClickListener {
            mainModel.isClipboardMode.value = !(mainModel.isClipboardMode.value ?: false)
            showSnackbar(fab, getString(if (mainModel.isClipboardMode.value ?:false) R.string.clipboard_mode_on else R.string.clipboard_mode_off))
        }

        copyBtn.setOnClickListener {
            //不允许复制的情况
            if (TextUtils.isEmpty(link_msg.text.toString())) {
                showSnackbar(fab, getString(R.string.pls_add_first))
                return@setOnClickListener
            }
            //开始复制
            (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip =
                    ClipData.newPlainText(mainModel.preferredServerUrl.get(), mainModel.preferredServerUrl.get())
            showSnackbar(fab, getString(R.string.url_clipboard))
        }

        qrcodeBtn.setOnClickListener {
            if (TextUtils.isEmpty(link_msg.text.toString())) {
                showSnackbar(fab, getString(R.string.pls_add_first))
                return@setOnClickListener
            }
            qrCodeDialog?.show()
        }
    }

    /**根据模式的不同，切换不同的组件显示状态*/
    fun refreshClipModeVisibility(isClipboardMode: Boolean) {
        runOnUiThread {
            copyBtn.visibility = if (isClipboardMode) View.GONE else View.VISIBLE
            clipboardBtn.setImageResource(if (isClipboardMode) R.drawable.ic_clipboard_on else R.drawable.ic_clipboard_off)
            clipboardContainer.visibility = if (isClipboardMode) View.VISIBLE else View.GONE
            fileNameContainer.visibility = if (isClipboardMode) View.GONE else View.VISIBLE
            if (isClipboardMode) {
                fab.hide()
                notiDominator.showNotifi()//剪切板模式必开通知，管你剪切板里有没有内容
            } else {
                fab.show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            mainModel.dealActivityResultIntent(data)
        }
    }

    /**二维码弹窗*/
    val qrCodeDialog: AlertDialog? by lazy {
        val view = LayoutInflater.from(this).inflate(R.layout.qrcode_dialog_layout, null, false)
        view.findViewById<View>(R.id.dismissBtn).setOnClickListener { qrCodeDialog?.dismiss() }
        val dialog = AlertDialog.Builder(this).setView(view)
                .setCancelable(false)
                .create()
        val window = dialog.getWindow()
        if (window != null) {
            window.setContentView(view)
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val p = window.attributes //获取对话框当前的参数值
            //                p.height = DensityUtil.dp2px(350);
            p.width = DensityUtil.dp2px(350f)
            window.attributes = p //设置生效
        }
        val qrcodeBmp = ZxingCodeTool.create2DCode(link_msg.getText().toString(), DensityUtil.dp2px(350f), DensityUtil.dp2px(350f))
        (dialog.findViewById<View>(R.id.qrcodeImage) as ImageView).setImageBitmap(qrcodeBmp)
        dialog
    }

    val mHandler: Handler by lazy {
        object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(inputMessage: Message) {
                when (inputMessage.what) {
                    HANDLER_CONNECTION_START -> {
                        val msg = String.format(getString(R.string.connected_ip), inputMessage.obj as String)
                        showSnackbar(findViewById(android.R.id.content), msg)
                    }
                    HANDLER_CONNECTION_END -> {
                        val msg2 = String.format(getString(R.string.disconnected_ip), inputMessage.obj as String)
                        showSnackbar(findViewById(android.R.id.content), msg2)
                    }
                    else -> super.handleMessage(inputMessage)
                }
            }
        }
    }

    private fun stopServer() {
        notiDominator.dismissAll(this)//隐藏通知
        mainModel.run {
            httpServer.value?.stopServer()
            httpServer.value = null
            clipServer.value?.stop()
            clipServer.value = null
        }
        MyHttpServer.clearFiles()
        //UI
        if (isFinishing) return
        mainModel.preferredServerUrl.set("")//IP栏不应该再显示IP
    }




    /**当切换成【剪切板】模式时，需要暂停而不是终止*/
    private fun switch2ClipServer() {
        mainModel.run {
            httpServer.value?.stopServer()
            httpServer.value = null

            startClipServer(assets)
            preferredServerUrl.set("http://${ClipServer.getIpAddress()}:1120")
            notiDominator.showNotifi()
        }
    }

    /**当换成【文件服务器】模式时*/
    private fun switch2FileServer() {
        //关剪切板
        mainModel.run {
            clipServer.value?.stop()
            clipServer.value = null

            startFileServer()
            preferredServerUrl.set("http://${ClipServer.getIpAddress()}:1120")
        }
        //如果恢复回来的时候没文件，那就取消通知、地址栏关掉
        if (MyHttpServer.getNormalUris().isEmpty()){
            notiDominator.dismissAll(this)
            mainModel.preferredServerUrl.set("")
        }
    }




    override fun onDestroy() {
        stopServer()
        super.onDestroy()
        System.exit(0)
    }

    override fun onResume() {
        super.onResume()
        //如果为true，那就再赋一次true，激发它的观察者，以刷新剪切板UI
        Handler().postDelayed( {
            if (mainModel.isClipboardMode.value ?: false){
                mainModel.isClipboardMode.value = true
            }
        },300)
    }

    /**
     * 6.0的权限
     */
    private fun grantPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            XXPermissions.with(this)
                    .constantRequest() //可设置被拒绝后继续申请，直到用户授权或者永久拒绝
                    //.permission(Permission.SYSTEM_ALERT_WINDOW, Permission.REQUEST_INSTALL_PACKAGES) //支持请求6.0悬浮窗权限8.0请求安装权限
                    //                    .permission(Permission.Group.STORAGE, Permission.Group.CALENDAR) //不指定权限则自动获取清单中的危险权限
                    .permission( //存储
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE //                    //电话
                            //                    Manifest.permission.CALL_PHONE,
                            //                            Manifest.permission.READ_PHONE_STATE
                            //                    //短信
                            //                    Manifest.permission.SEND_SMS,
                            //                    //通讯录
                            //                    Manifest.permission.READ_PHONE_NUMBERS,
                            //                    Manifest.permission.GET_ACCOUNTS
                            //                            Manifest.permission.READ_CONTACTS
                            //                    //定位
                            //                    Manifest.permission.ACCESS_COARSE_LOCATION,
                            //                    Manifest.permission.ACCESS_FINE_LOCATION,
                            //                    //相机
                            //                    Manifest.permission.CAMERA
                            //                                Manifest.permission.CHANGE_NETWORK_STATE,
                            //                                Manifest.permission.WRITE_SETTINGS
                            //安装APK
                            /*Manifest.permission.REQUEST_INSTALL_PACKAGES*/
                    ).request(object : OnPermission {
                        override fun hasPermission(granted: List<String>, isAll: Boolean) {
                            if (isAll) {
                                BatteryOptiDominator.requestIgnoreBatteryOpti(this@MainActivity)
                            }
                        }

                        override fun noPermission(denied: List<String>, quick: Boolean) {
                            if (quick) {
                                androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                                        .setMessage("请允许权限")
                                        .setPositiveButton("好的") { dialog, which -> XXPermissions.gotoPermissionSettings(this@MainActivity) }
                                        .show()
                            }
                        }
                    })
        }
    }
}
