package com.sibyl.HttpFileDominator.activities;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.sibyl.HttpFileDominator.BuildConfig;
import com.sibyl.HttpFileDominator.MyHttpServer;
import com.sibyl.HttpFileDominator.R;
import com.sibyl.HttpFileDominator.UriInterpretation;

import java.util.ArrayList;
import java.util.Iterator;

public class MainActivity extends BaseActivity {

    public static final int REQUEST_CODE = 1024;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setNavigationBarColor(getResources().getColor(R.color.main_activity_background_color, null));

        setupToolbar();
        setupTextViews();
        setupNavigationViews();
        createViewClickListener();
        setupPickItemView();
        // debugSendFileActivity();


        ArrayList<UriInterpretation> sharedUriList = getIntentFileUris(getIntent());
        if (sharedUriList != null) {
            dealWithNewUris(sharedUriList);
        }
    }

    @Override
    protected void onNewIntent(Intent newIntent) {
        super.onNewIntent(newIntent);
        //获取从外面直接分享进来的文件
        ArrayList<UriInterpretation> sharedUriList = getIntentFileUris(newIntent);
        if (sharedUriList != null) {
            dealWithNewUris(sharedUriList);
        }
    }

    private void debugSendFileActivity() {
        if (!BuildConfig.BUILD_TYPE.equals("release")) {    // this should not happen
            String path = "/mnt/sdcard/m.txt";

            Intent intent = new Intent(this, SendFileActivity.class);
            intent.addCategory("android.intent.category.DEFAULT");
            intent.putExtra(Intent.EXTRA_TEXT, path);
            // intent.setType("inode/directory");

            startActivity(intent);
        }
    }

    private void setupPickItemView() {
        findViewById(R.id.fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                intent.setType("*/*");
                startActivityForResult(intent, REQUEST_CODE);
            }
        });
//        findViewById(R.id.pick_items).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
////                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//                    intent.addCategory(Intent.CATEGORY_OPENABLE);
//                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
//                    intent.setType("*/*");
//                    startActivityForResult(intent, REQUEST_CODE);
////                } else {
////                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
////                    intent.setType("*/*");
////                    startActivityForResult(intent, REQUEST_CODE);
////                }
//            }
//        });
    }

//    private void setViewsVisible() {
//        findViewById(R.id.link_layout).setVisibility(View.VISIBLE);
////        findViewById(R.id.navigation_layout).setVisibility(View.VISIBLE);
//    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            dealWithNewUris(getFileUris(data));
        }
    }

    private void dealWithNewUris(ArrayList<UriInterpretation> uriList) {
        //先过滤一波已经添加过的
        Iterator<UriInterpretation> it = uriList.iterator();
        while (it.hasNext()) {
            UriInterpretation uriInterpretation = it.next();
            if (!MyHttpServer.GetFiles().isEmpty() && MyHttpServer.GetFiles().contains(uriInterpretation)) {
                it.remove();
            }
        }
        //如果没有建立起http服务时==========
        if (httpServer == null) {
            initHttpServer(uriList);
            addNewFlex2UI(uriList);//显示文件名到textView
//            saveServerUrlToClipboard();
            showIPText();
//            setViewsVisible();
            return;
        }
        //如果已经建立起http服务了，那只需要添加文件并显示即可
        MyHttpServer.GetFiles().addAll(uriList);
        addNewFlex2UI(uriList);//显示出来
    }


    /**
     * 文件Flex布局UI展示
     */
    protected void addNewFlex2UI(ArrayList<UriInterpretation> newUriList) {
        for (UriInterpretation uriInterpretation : newUriList) {
            final View view = LayoutInflater.from(this).inflate(R.layout.flex_item, flexboxLayout, false);
            view.setTag(uriInterpretation);//把uriInterpretation保存到tag，到时候点击时用它来找到实时的index
            ((TextView) view.findViewById(R.id.itemNameTv)).setText(uriInterpretation.getPath());
            ((ImageButton) view.findViewById(R.id.itemDeleteBtn)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int deleteIndex = MyHttpServer.GetFiles().indexOf((UriInterpretation) view.getTag());
                    if (deleteIndex != -1) {
                        flexboxLayout.removeViewAt(deleteIndex);
                        MyHttpServer.GetFiles().remove(deleteIndex);
                        //如果删光了
                        if (MyHttpServer.GetFiles().isEmpty()) {
                            link_msg.setText("");
                            stopServer();
                            Snackbar.make(fab, getString(R.string.files_clear_stop_server), Snackbar.LENGTH_LONG).show();
                        }
                    }
                }
            });
            flexboxLayout.addView(view);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        // Read values from the "savedInstanceState"-object and put them in your textview
        super.onRestoreInstanceState(savedInstanceState);
//        uriPath.setText(savedInstanceState.getCharSequence("uriPath"));
        link_msg.setText(savedInstanceState.getCharSequence("link_msg"));
        ArrayList<UriInterpretation> uriInterpretations = new ArrayList<>();
        for (Parcelable item : savedInstanceState.getParcelableArrayList("uriCaches")) {
            uriInterpretations.add(new UriInterpretation((Uri) item, this.getContentResolver()));
        }
        dealWithNewUris(uriInterpretations);

//        if (!"".equals(savedInstanceState.getCharSequence("uriPath"))) {
//            setViewsVisible();
//        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putCharSequence("link_msg", link_msg.getText());
//        outState.putCharSequence("uriPath", uriPath.getText());
        // Save the values you need from your textview into "outState"-object
        ArrayList<Uri> uriCaches = new ArrayList<>();
        for (UriInterpretation item : MyHttpServer.GetFiles()) {
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
                Snackbar.make(findViewById(android.R.id.content), "Error obtaining the file path", Snackbar.LENGTH_LONG).show();
                return null;
            }

            myUri = Uri.parse(tempString);

            if (myUri == null) {
                Snackbar.make(findViewById(android.R.id.content), "Error obtaining the file path", Snackbar.LENGTH_LONG).show();
                return null;
            }
        }

        theUris.add(new UriInterpretation(myUri, this.getContentResolver()));
        return theUris;
    }

    private ArrayList<UriInterpretation> getUrisForActionSendMultiple(Intent dataIntent, ArrayList<UriInterpretation> theUris) {
        ArrayList<Parcelable> list = dataIntent
                .getParcelableArrayListExtra(Intent.EXTRA_STREAM);
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
}
