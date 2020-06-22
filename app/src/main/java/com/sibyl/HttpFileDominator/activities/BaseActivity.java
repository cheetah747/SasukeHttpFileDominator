package com.sibyl.HttpFileDominator.activities;

import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.sibyl.HttpFileDominator.BuildConfig;
import com.sibyl.HttpFileDominator.LoadWaitDominator;
import com.sibyl.HttpFileDominator.MyHttpServer;
import com.sibyl.HttpFileDominator.R;
import com.sibyl.HttpFileDominator.UriInterpretation;
import com.sibyl.HttpFileDominator.utils.DensityUtil;
import com.sibyl.HttpFileDominator.utils.MySnackbarKt;
import com.sibyl.HttpFileDominator.utils.ZxingCodeTool;
import com.sibyl.HttpFileDominator.views.DisplayRawFileFragment;

import java.util.ArrayList;

public class BaseActivity extends AppCompatActivity {

    public static final int HANDLER_CONNECTION_START = 42;
    public static final int HANDLER_CONNECTION_END = 4242;
    protected static MyHttpServer httpServer = null;
    protected String preferredServerUrl;
    protected CharSequence[] listOfServerUris;
    // LinkMessageView
    protected TextView link_msg;
    protected TextView uriPath;
    protected FloatingActionButton fab;
    protected FlexboxLayout fileNameContainer;

    protected ImageButton qrcodeBtn;
//    protected ImageButton clipboardBtn;
    protected ImageButton copyBtn;

    protected FlexboxLayout flexboxLayout;
    protected FlexboxLayout clipboardLayout;
    protected LoadWaitDominator loadWait;
    // NavigationViews
//    protected View bttnQrCode;
////    protected View stopServer;
//    protected View share;
//    protected View changeIp;

    private Handler mHandler;

    private AlertDialog qrCodeDialog;


    public void sendConnectionStartMessage(String ipAddress) {
        Log.d("mm", "begin: " + ipAddress  + " " + this);
        mHandler.handleMessage(mHandler.obtainMessage(BaseActivity.HANDLER_CONNECTION_START, ipAddress));
    }

    public void sendConnectionEndMessage(String ipAddress) {
        Log.d("mm", "end: " + ipAddress + " " + this);
        mHandler.handleMessage(mHandler.obtainMessage(BaseActivity.HANDLER_CONNECTION_END, ipAddress));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler(Looper.getMainLooper()) {

            @Override
            public void handleMessage(Message inputMessage) {
                switch (inputMessage.what) {
                    case HANDLER_CONNECTION_START:
                        String msg = String.format(getString(R.string.connected_ip), (String) inputMessage.obj);
//                        Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_LONG).show();
                        MySnackbarKt.show(findViewById(android.R.id.content),msg);
                        break;
                    case HANDLER_CONNECTION_END:
                        String msg2 = String.format(getString(R.string.disconnected_ip), (String) inputMessage.obj);
//                        Snackbar.make(findViewById(android.R.id.content), msg2, Snackbar.LENGTH_LONG).show();
                        MySnackbarKt.show(findViewById(android.R.id.content),msg2);
                        break;
                    default:
                        super.handleMessage(inputMessage);
                }
            }
        };
    }

    protected void setupToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.app_name));
        toolbar.setTitleTextColor(getResources().getColor(R.color.light_blue));
        setSupportActionBar(toolbar);
    }

    protected void setupTextViews() {
        loadWait = new LoadWaitDominator(this, (RelativeLayout) findViewById(R.id.containerLayout));
        link_msg = (TextView) findViewById(R.id.link_msg);
        fileNameContainer = (FlexboxLayout) findViewById(R.id.fileNameContainer);
        clipboardLayout = (FlexboxLayout) findViewById(R.id.clipboardContainer);
//        uriPath = (TextView) findViewById(R.id.uriPath);
        fab = (FloatingActionButton) findViewById(R.id.fab);

    }

    protected void setupNavigationViews() {
//        bttnQrCode = findViewById(R.id.button_qr_code);
////        stopServer = findViewById(R.id.stop_server);
//        share = findViewById(R.id.button_share_url);
//        changeIp = findViewById(R.id.change_ip);
        qrcodeBtn = findViewById(R.id.qrcodeBtn);
//        clipboardBtn = findViewById(R.id.clipboardBtn);
        copyBtn = findViewById(R.id.copyBtn);
        flexboxLayout = findViewById(R.id.fileNameContainer);
    }

    protected void createViewClickListener() {
        qrcodeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TextUtils.isEmpty(link_msg.getText().toString())) {
//                    Snackbar.make(fab, getString(R.string.pls_add_first), Snackbar.LENGTH_LONG).show();
                    MySnackbarKt.show(fab,getString(R.string.pls_add_first));
                    return;
                }
                generateBarCodeIfPossible();
            }
        });

//        stopServer.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                MyHttpServer p = httpServer;
//                httpServer = null;
//                if (p != null) {
//                    p.stopServer();
//                }
//                Snackbar.make(findViewById(android.R.id.content), getString(R.string.now_sharing_anymore), Snackbar.LENGTH_SHORT).show();
//            }
//        });
//
//        shareBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (TextUtils.isEmpty(link_msg.getText().toString())) {
//                    Snackbar.make(fab, getString(R.string.pls_add_first), Snackbar.LENGTH_LONG).show();
//                    return;
//                }
//                Intent i = new Intent(Intent.ACTION_SEND);
//                i.setType("text/plain");
//                i.putExtra(Intent.EXTRA_TEXT, preferredServerUrl);
//                startActivity(Intent.createChooser(i, BaseActivity.this.getString(R.string.share_url)));
//            }
//        });

        link_msg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TextUtils.isEmpty(link_msg.getText().toString())){
//                    Snackbar.make(fab, getString(R.string.pls_add_first), Snackbar.LENGTH_LONG).show();
                    MySnackbarKt.show(fab,getString(R.string.pls_add_first));
                    return;
                }
                createChangeIpDialog();
            }
        });

        copyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(link_msg.getText().toString())) {
//                    Snackbar.make(fab, getString(R.string.pls_add_first), Snackbar.LENGTH_LONG).show();
                    MySnackbarKt.show(fab,getString(R.string.pls_add_first));
                    return;
                }
                saveServerUrlToClipboard();
            }
        });
    }




    public void generateBarCodeIfPossible() {
        if (qrCodeDialog == null){
            View view = LayoutInflater.from(this).inflate(R.layout.qrcode_dialog_layout,null,false);
            (view.findViewById(R.id.dismissBtn)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    qrCodeDialog.dismiss();
                }
            });
            qrCodeDialog = new AlertDialog.Builder(this).setView(view)
                    .setCancelable(false)
                    .create();
            Window window = qrCodeDialog.getWindow();
            if (window != null){
                window.setContentView(view);
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                WindowManager.LayoutParams p = window.getAttributes();//获取对话框当前的参数值
//                p.height = DensityUtil.dp2px(350);
                p.width = DensityUtil.dp2px(350);
                window.setAttributes(p);//设置生效
            }
        }
        ((ImageView)qrCodeDialog.findViewById(R.id.qrcodeImage)).setImageBitmap(transUrlTo2DCode(link_msg.getText().toString()));
        qrCodeDialog.show();
//        Intent intent = new Intent("com.google.zxing.client.android.ENCODE");
//        intent.putExtra("ENCODE_TYPE", "TEXT_TYPE");
//        intent.putExtra("ENCODE_DATA", link_msg.getText().toString());
//        try {
//            startActivity(intent);
//        } catch (ActivityNotFoundException e) {
//            Toast.makeText(this, "You need to download the Barcode Scanner to generate QR Codes", Toast.LENGTH_LONG).show();
//            openInPlayStore("com.google.zxing.client.android");
//        }
    }

    /**把Url生成二维码*/
    public Bitmap transUrlTo2DCode(String url){
        if(url != null){
            return ZxingCodeTool.create2DCode(url, DensityUtil.dp2px(350), DensityUtil.dp2px(350));
        }
        return null;
    }

    private void createChangeIpDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle(R.string.change_ip);
        b.setSingleChoiceItems(listOfServerUris, 0,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        preferredServerUrl = listOfServerUris[whichButton]
                                .toString();
//                        saveServerUrlToClipboard();
                        showIPText();
                        dialog.dismiss();
                    }
                });
        b.create().show();
    }

    /**不用了*/
    protected void showUriNames(ArrayList<UriInterpretation> allUriList) {
//        thisUriInterpretation.getPath();
//        StringBuilder output = new StringBuilder();
//        String sep = "\n";
//        boolean first = true;
//        for (UriInterpretation thisUriInterpretation : uriList) {
//            if (first) {
//                first = false;
//            } else {
//                output.append(sep);
//            }
//            output.append(thisUriInterpretation.getPath());
//        }
//        uriPath.setText(output.toString());
    }

    protected void saveServerUrlToClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(preferredServerUrl, preferredServerUrl));

//        Snackbar.make(fab, getString(R.string.url_clipboard), Snackbar.LENGTH_LONG).show();
        MySnackbarKt.show(fab,getString(R.string.url_clipboard));

//        Snackbar.make(findViewById(android.R.id.content), getString(R.string.url_clipboard), Snackbar.LENGTH_LONG).show();
    }

    //下划线
    protected void showIPText() {
        if (isFinishing()) return;
//        link_msg.setPaintFlags(link_msg.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                link_msg.setText(preferredServerUrl);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    private void showPrivacyPolicy() {
        DialogFragment newFragment = DisplayRawFileFragment.newInstance(getString(R.string.privacy_policy), R.raw.privacy_policy);
        newFragment.show(getFragmentManager(), "dialog");
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_rate_app:
                rate_this_app();
                return super.onOptionsItemSelected(item);
            case R.id.action_privacy_policy:
                showPrivacyPolicy();
                return super.onOptionsItemSelected(item);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void rate_this_app() {
        String appName = BuildConfig.APPLICATION_ID;
        openInPlayStore(appName);
    }

    private void openInPlayStore(String appName) {
        String theUrl = "market://details?id=" + appName;
        Intent browse = new Intent(Intent.ACTION_VIEW, Uri.parse(theUrl));
        startActivity(browse);
    }
}
