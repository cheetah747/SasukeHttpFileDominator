package com.sibyl.httpfiledominator.activities;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.hjq.permissions.OnPermission;
import com.hjq.permissions.XXPermissions;
import com.sibyl.httpfiledominator.LoadWaitDominator;
import com.sibyl.httpfiledominator.MyHttpServer;
import com.sibyl.httpfiledominator.R;
import com.sibyl.httpfiledominator.UriInterpretation;
import com.sibyl.httpfiledominator.utils.BatteryOptiDominator;
import com.sibyl.httpfiledominator.utils.ClipboardUtil;
import com.sibyl.httpfiledominator.utils.JsonDominator;
import com.sibyl.httpfiledominator.utils.MySnackbarKt;
import com.sibyl.httpfiledominator.utils.NotiDominator;
import com.sibyl.httpfiledominator.utils.ThreadManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MainActivity0 extends BaseActivity0 {

    public static final int REQUEST_CODE = 1024;

    private NotiDominator notiDominator;

    private boolean isClipboardMode = false;//是否是剪切板模式

    ImageButton clipboardBtn;

    FloatingActionButton fab;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent().getBooleanExtra("isStopServer", false)) {
            stopServer();
            finish();
        }
        setContentView(R.layout.activity_main);
        getWindow().setNavigationBarColor(getResources().getColor(R.color.main_activity_background_color, null));
        setupToolbar();
        setupTextViews();

        grantPermissions();//操你妈6.0权限

        setupNavigationViews();
        createViewClickListener();
        initViews();
        // debugSendFileActivity();


        ThreadManager.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                if (getIntent().getExtras() != null) {
                    loadWait.show(LoadWaitDominator.LOADING);
                }
                //注：以下是耗时操作
                ArrayList<UriInterpretation> sharedUriList = getIntentFileUris(getIntent());
                if (sharedUriList != null) {
                    dealWithNewUris(sharedUriList);//在 onCreate() 里
                }
            }
        });
    }

    @Override
    protected void onNewIntent(final Intent newIntent) {
        super.onNewIntent(newIntent);
        if (newIntent.getBooleanExtra("isStopServer",false)) {
            newIntent.putExtra("isStopServer",false);
            stopServer();
            finish();
        }
        //获取从外面直接分享进来的文件
        ThreadManager.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                if (newIntent.getExtras() != null) {
                    loadWait.show(LoadWaitDominator.LOADING);
                }
                //注：以下是耗时操作
                final ArrayList<UriInterpretation> sharedUriList = getIntentFileUris(newIntent);
                if (sharedUriList != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dealWithNewUris(sharedUriList);//在 onNewIntent() 里
                        }
                    });
                }
            }
        });
    }

    private void initViews() {
        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                intent.setType("*/*");
                startActivityForResult(intent, REQUEST_CODE);
            }
        });

        //共享剪切板模式
        clipboardBtn = findViewById(R.id.clipboardBtn);
        clipboardBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isClipboardMode = !isClipboardMode;
                refreshClipModeVisibility();
//                Snackbar.make(fab, getString(isClipboardMode ? R.string.clipboard_mode_on : R.string.clipboard_mode_off), Snackbar.LENGTH_LONG).show();
                MySnackbarKt.show(fab,getString(isClipboardMode ? R.string.clipboard_mode_on : R.string.clipboard_mode_off));
                dealChangeMode();//点击切换模式按钮时
            }
        });
        //初始刷
        refreshClipModeVisibility();//在 initViews() 里
    }

    /**
     * 切换 剪切板模式 和 普通模式 时的处理。
     */
    public void dealChangeMode(){
        ArrayList<UriInterpretation> tempUris = MyHttpServer.getNormalUris();//先加载默认模式的数据
        if (isClipboardMode){
            //剪切板内容写入到文件
            JsonDominator.fire2Dir(ClipboardUtil.getText(MainActivity0.this), getClipboardFile());
            tempUris = new ArrayList<>();
            tempUris.add(new UriInterpretation(Uri.fromFile(getClipboardFile()), ClipboardUtil.getText(MainActivity0.this), getContentResolver()));
            MyHttpServer.setClipboardUris(tempUris);
        }

        if (httpServer == null) {
            initHttpServer(tempUris);//在dealChangeMode()中
        } /*else {//如果已经建立起http服务了，那只需要添加文件并显示即可
            MyHttpServer.changeUrisByMode(isClipboardMode);
        }*/
        MyHttpServer.changeUrisByMode(isClipboardMode);

        flexboxLayout.removeAllViews();
        addNewFlex2UI(tempUris,loadWait);
    }

    /**获取剪切板缓存文件*/
    private File getClipboardFile(){
        return new File(getExternalFilesDir(null).getAbsolutePath() + File.separator + "clipboard.txt");
    }

    public void refreshClipModeVisibility(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                clipboardBtn.setImageResource(isClipboardMode? R.drawable.ic_clipboard_on : R.drawable.ic_clipboard_off );
                if (isClipboardMode){
                    fab.hide();
                }else{
                    fab.show();
                }
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            ThreadManager.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    if (data != null) {
                        loadWait.show(LoadWaitDominator.LOADING);
                    }
                    ArrayList<UriInterpretation> fileUris = getFileUris(data);
                    if (fileUris != null && !fileUris.isEmpty()) {
                        dealWithNewUris(fileUris);//在 onActivityResult() 里
                    }
                }
            });
        }
    }

    private void dealWithNewUris(ArrayList<UriInterpretation> newUriList) {
        if (isFinishing() || newUriList == null || newUriList.isEmpty()) return;
        //先过滤一波已经添加过的
        Iterator<UriInterpretation> it = newUriList.iterator();
        while (it.hasNext()) {
            UriInterpretation uriInterpretation = it.next();
            if (!MyHttpServer.getNormalUris().isEmpty() && MyHttpServer.getNormalUris().contains(uriInterpretation)) {
                it.remove();
            }
        }
        //如果没有建立起http服务时==========
        if (httpServer == null) {
            initHttpServer(newUriList);//在 dealWithNewUris() 中
        } /*else {//如果已经建立起http服务了，那只需要添加文件并显示即可
            MyHttpServer.getNormalUris().addAll(newUriList);
        }*/
        MyHttpServer.getNormalUris().addAll(newUriList);
        //切换到常规模式
        if (isClipboardMode){
            flexboxLayout.removeAllViews();
        }
        isClipboardMode = false;
        addNewFlex2UI(newUriList, loadWait);
        MyHttpServer.changeUrisByMode(isClipboardMode);
        refreshClipModeVisibility();//在 dealWithNewUris() 里
        //显示到UI

    }


    /**
     * 文件Flex布局UI展示
     */
    protected void addNewFlex2UI(final ArrayList<UriInterpretation> newUriList, final LoadWaitDominator loadWait) {
        if (isFinishing()) return;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //如果是剪切板模式
                if (newUriList.size() == 1 && newUriList.get(0).isClipboardType){
                    final View view = LayoutInflater.from(MainActivity0.this).inflate(R.layout.flex_item_files, flexboxLayout, false);
                    view.findViewById(R.id.divider).setVisibility(View.GONE);
                    String showText = newUriList.get(0).clipboardText.isEmpty()?"（剪切板为空）" : newUriList.get(0).clipboardText;
                    ((TextView) view.findViewById(R.id.itemNameTv)).setText(showText);
                    view.findViewById(R.id.itemDeleteBtn).setVisibility(View.GONE);//不需要叉叉
                    flexboxLayout.addView(view);
                    loadWait.dismissAll();
                    return;
                }
                //如果是普通模式
                for (UriInterpretation uriInterpretation : newUriList) {
                    final View view = LayoutInflater.from(MainActivity0.this).inflate(R.layout.flex_item_files, flexboxLayout, false);
                    view.setTag(uriInterpretation);//把uriInterpretation保存到tag，到时候点击时用它来找到实时的index
                    ((TextView) view.findViewById(R.id.itemNameTv)).setText(uriInterpretation.getPath());
                    ((ImageView) view.findViewById(R.id.isFolderIcon)).setVisibility(uriInterpretation.isDirectory() ? View.VISIBLE : View.GONE);
                    ((ImageButton) view.findViewById(R.id.itemDeleteBtn)).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            int deleteIndex = MyHttpServer.getNormalUris().indexOf((UriInterpretation) view.getTag());
                            if (deleteIndex != -1) {
                                flexboxLayout.removeViewAt(deleteIndex);
                                MyHttpServer.getNormalUris().remove(deleteIndex);
                                //如果删光了
                                if (MyHttpServer.getNormalUris().isEmpty()) {
                                    link_msg.setText("");
                                    stopServer();
//                                    Snackbar.make(fab, getString(R.string.files_clear_stop_server), Snackbar.LENGTH_LONG).show();
                                    MySnackbarKt.show(fab,getString(R.string.files_clear_stop_server));
                                }
                            }
                        }
                    });
                    flexboxLayout.addView(view);
                }
                loadWait.dismissAll();
            }
        });
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        // Read values from the "savedInstanceState"-object and put them in your textview
        super.onRestoreInstanceState(savedInstanceState);
//        uriPath.setText(savedInstanceState.getCharSequence("uriPath"));
        link_msg.setText(savedInstanceState.getCharSequence("link_msg"));
        isClipboardMode = savedInstanceState.getBoolean("isClipboardMode");

        ArrayList<UriInterpretation> uriInterpretations = new ArrayList<>();
        for (Parcelable item : savedInstanceState.getParcelableArrayList("uriCaches")) {
            uriInterpretations.add(new UriInterpretation((Uri) item, this.getContentResolver()));
        }

        MyHttpServer.setNormalUris(uriInterpretations);
        refreshClipModeVisibility();//在 onRestoreInstanceState() 里
        //必须要在主线程里，否则获取剪切板是空的
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                dealChangeMode();//通过 onRestoreInstanceState() 恢复时
            }
        });

//        dealWithNewUris(uriInterpretations);

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putCharSequence("link_msg", link_msg.getText());
        outState.putBoolean("isClipboardMode",isClipboardMode);
        ArrayList<Uri> uriCaches = new ArrayList<>();
        for (UriInterpretation item : MyHttpServer.getNormalUris()) {
            uriCaches.add(item.getUri());
        }
        outState.putParcelableArrayList("uriCaches", uriCaches);
        super.onSaveInstanceState(outState);
    }


    private ArrayList<UriInterpretation> getFileUris(Intent data) {
        ArrayList<UriInterpretation> theUris = new ArrayList<UriInterpretation>();
        Uri dataUri = data.getData();
        if (dataUri != null) {
            theUris.add(new UriInterpretation(dataUri, this.getContentResolver()));
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                getFileUrisFromClipboard(data, theUris);
            }
        }
        return theUris;
    }

    @Override
    protected void onDestroy() {
        stopServer();
        super.onDestroy();
    }

    /**
     * 退出应用后就停止服务
     */
    private void stopServer() {
        if (notiDominator != null){
            notiDominator.dismissAll(this);
        }
        MyHttpServer p = httpServer;
        httpServer = null;
        if (p != null) {
            p.stopServer();
        }
        MyHttpServer.clearFiles();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void getFileUrisFromClipboard(Intent data, ArrayList<UriInterpretation> theUris) {
        ClipData clipData = data.getClipData();
        for (int i = 0; i < clipData.getItemCount(); ++i) {
            ClipData.Item item = clipData.getItemAt(i);
            Uri uri = item.getUri();
            theUris.add(new UriInterpretation(uri, this.getContentResolver()));
        }
    }

    /**
     * 以下是从SendFileActivity里拷过来的。用来接收直接从外面分享进来的文件，
     * 原SendFileActivity已废弃
     */
    private ArrayList<UriInterpretation> getIntentFileUris(Intent dataIntent) {
        if (isFinishing()) return null;
//        Intent dataIntent = getIntent();
        ArrayList<UriInterpretation> theUris = new ArrayList<>();

        if (Intent.ACTION_SEND_MULTIPLE.equals(dataIntent.getAction())) {
            return getUrisForActionSendMultiple(dataIntent, theUris);
        }

        Bundle extras = dataIntent.getExtras();
        if (extras == null) {//直接手动进的主页，不会存在传intent数据的情况
            return null;
        }

        Uri myUri = (Uri) extras.get(Intent.EXTRA_STREAM);

        if (myUri == null) {
            String tempString = (String) extras.get(Intent.EXTRA_TEXT);
            if (tempString == null) {
//                Snackbar.make(findViewById(android.R.id.content), "Error obtaining the file path", Snackbar.LENGTH_LONG).show();
                MySnackbarKt.show(findViewById(android.R.id.content),"Error obtaining the file path");
                return null;
            }

            myUri = Uri.parse(tempString);

            if (myUri == null) {
//                Snackbar.make(findViewById(android.R.id.content), "Error obtaining the file path", Snackbar.LENGTH_LONG).show();
                MySnackbarKt.show(findViewById(android.R.id.content),"Error obtaining the file path");
                return null;
            }
        }

        theUris.add(new UriInterpretation(myUri, this.getContentResolver()));
        return theUris;
    }

    private ArrayList<UriInterpretation> getUrisForActionSendMultiple(Intent dataIntent, ArrayList<UriInterpretation> theUris) {
        ArrayList<Parcelable> list = dataIntent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        if (list != null) {
            for (Parcelable parcelable : list) {
                Uri stream = (Uri) parcelable;
                if (stream != null) {
                    theUris.add(new UriInterpretation(stream, this.getContentResolver()));
                }
            }
        }
        return theUris;
    }


    /**
     * 6.0的权限
     */
    private void grantPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            XXPermissions.with(this)
                    .constantRequest() //可设置被拒绝后继续申请，直到用户授权或者永久拒绝
                    //.permission(Permission.SYSTEM_ALERT_WINDOW, Permission.REQUEST_INSTALL_PACKAGES) //支持请求6.0悬浮窗权限8.0请求安装权限
                    //                    .permission(Permission.Group.STORAGE, Permission.Group.CALENDAR) //不指定权限则自动获取清单中的危险权限
                    .permission(//存储
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE
//                    //电话
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
                    ).request(new OnPermission() {
                @Override
                public void hasPermission(List<String> granted, boolean isAll) {
                    if (isAll) {
                        BatteryOptiDominator.requestIgnoreBatteryOpti(MainActivity0.this);
                    }
                }

                @Override
                public void noPermission(List<String> denied, boolean quick) {
                    if (quick) {
                        new AlertDialog.Builder(MainActivity0.this)
                                .setMessage("请允许权限")
                                .setPositiveButton("好的", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        XXPermissions.gotoPermissionSettings(MainActivity0.this);
                                    }
                                })
                                .show();
                    }
                }
            });
//                    .request(object :OnPermission {
//                override fun hasPermission(granted:List<String>, isAll: Boolean) {}
//
//                override fun noPermission(denied: List<String>, quick: Boolean) {
//                    if (quick) {
//                        android.app.AlertDialog.Builder(this@MainActivity)
//                                .setMessage(resources.getString(R.string.permission_allow))
//                                .setPositiveButton(resources.getString(R.string.go_now)) { dialog, which -> XXPermissions.gotoPermissionSettings(this@MainActivity) }
//                                .show()
//                    }
//                }
//            })
        }
    }


    protected void initHttpServer(ArrayList<UriInterpretation> myUris) {
        if (myUris == null || myUris.size() == 0) {
//            finish();//你傻啊，自己把页面关掉干嘛？？？？有病 ？？？？操你妈
            return;
        }
        if (notiDominator == null){
            notiDominator = new NotiDominator(this);
        }
        notiDominator.showNotifi();
        httpServer = new MyHttpServer(1120);
        //注释掉防报错
//        listOfServerUris = httpServer.listOfIpAddresses();
        preferredServerUrl = listOfServerUris[0].toString();

        showIPText();
//        httpServer.setBaseActivity(this);
//        httpServer.setFiles(myUris);


    }


}
