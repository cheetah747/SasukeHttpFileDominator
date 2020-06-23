package com.sibyl.httpfiledominator.views;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import com.sibyl.httpfiledominator.R;

/**
 * Created by Sasuke on 2017/7/27.
 * 封装通用Popupwindow （http://blog.csdn.net/u011418943/article/details/74908772）
 *
 * example:
 *
 * photoPopup = CustomPopupWindow.Builder(this)
                     .setRoot(root)
                     .setwidth(ViewGroup.LayoutParams.MATCH_PARENT)
                     .setheight(ViewGroup.LayoutParams.MATCH_PARENT)
                     .setContentView(R.layout.photo_preview_pop_ll) //设置布局文件
                     .builder()
 photoPopup.getItemView(R.id.popupRoot).setOnClickListener { photoPopup.dismiss() }
 */

public class CustomPopupWindow {
    private PopupWindow mPopupWindow;
    private View contentview;
    private Context mContext;
    private Object tag;

    public void setTag(Object tag){
        this.tag = tag;
    }

    public Object getTag(){
        return tag;
    }

    public CustomPopupWindow(Builder builder) {
        mContext = builder.context;
        if (builder.root != null) {
            contentview = LayoutInflater.from(mContext).inflate(builder.contentviewid, builder.root, false);
        }else{
            contentview = LayoutInflater.from(mContext).inflate(builder.contentviewid, null);
        }
        if (builder.contentview != null){
            contentview = builder.contentview;
        }

        mPopupWindow =
                new PopupWindow(contentview, builder.width, builder.height, builder.fouse);

        //需要跟 setBackGroundDrawable 结合
        mPopupWindow.setOutsideTouchable(builder.outsidecancel);
        mPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        if(builder.bitmapDrawable != null){
            mPopupWindow.setBackgroundDrawable(builder.bitmapDrawable);
        }
        mPopupWindow.setAnimationStyle(builder.animstyle);
    }

    /**
     * popup 消失
     */
    public void dismiss() {
        if (mPopupWindow != null) {
            mPopupWindow.dismiss();
        }
    }

    /**
     * 根据id获取view
     *
     * @param viewid
     * @return
     */
    public View getItemView(int viewid) {
        if (mPopupWindow != null) {
            return this.contentview.findViewById(viewid);
        }
        return null;
    }

    /**
     * 根据父布局，显示位置
     *
     * @param rootview
     * @param gravity
     * @param x
     * @param y
     * @return
     */
    public CustomPopupWindow showAtLocation(View rootview, int gravity, int x, int y) {
        if (mPopupWindow != null) {
//            View rootview = LayoutInflater.from(mContext).inflate(rootviewid, null);
            mPopupWindow.showAtLocation(rootview, gravity, x, y);
        }
        return this;
    }

    /**
     * 根据id获取view ，并显示在该view的位置
     *
     * @param targetviewId
     * @param gravity
     * @param offx
     * @param offy
     * @return
     */
    public CustomPopupWindow showAsLocation(int targetviewId, int gravity, int offx, int offy) {
        if (mPopupWindow != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            View targetview = LayoutInflater.from(mContext).inflate(targetviewId, null);
            mPopupWindow.showAsDropDown(targetview, gravity, offx, offy);
        }
        return this;
    }

    /**
     * 显示在 targetview 的不同位置
     *
     * @param targetview
     * @param gravity
     * @param offx
     * @param offy
     * @return
     */
    public CustomPopupWindow showAsLocation(View targetview, int gravity, int offx, int offy) {
        if (mPopupWindow != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mPopupWindow.showAsDropDown(targetview, gravity, offx, offy);
        }
        return this;
    }


    /**
     * 根据id设置焦点监听
     *
     * @param viewid
     * @param listener
     */
    public void setOnFocusListener(int viewid, View.OnFocusChangeListener listener) {
        View view = getItemView(viewid);
        view.setOnFocusChangeListener(listener);
    }

    public int getHeight(){
        return mPopupWindow.getHeight();
    }

    /**
     * builder 类
     */
    public static class Builder {
        private int contentviewid;
        private View contentview;
        private int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        private int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        private boolean fouse = true;//默认可获取焦点
        private boolean outsidecancel = true;//默认外面可点击消除
        private BitmapDrawable bitmapDrawable;
        private int animstyle = R.style.popup_anim_style;//默认动画
        private Context context;
        private ViewGroup root;//根布局

        public Builder(Context context) {
            this.context = context;
        }

        public Builder setContentView(int contentviewid) {
            this.contentviewid = contentviewid;
            return this;
        }

        public Builder setContentView(View contentview) {
            this.contentview = contentview;
            return this;
        }

        public Builder setRoot(ViewGroup v) {
            this.root = v;
            return this;
        }

        public Builder setwidth(int width) {
            this.width = width;
            return this;
        }

        public Builder setheight(int height) {
            this.height = height;
            return this;
        }

        public Builder setFouse(boolean fouse) {
            this.fouse = fouse;
            return this;
        }

        public Builder setOutSideCancel(boolean outsidecancel) {
            this.outsidecancel = outsidecancel;
            return this;
        }

        public Builder setBackgroundDrawable(BitmapDrawable bitmapDrawable){
            this.bitmapDrawable = bitmapDrawable;
            return this;
        }

        public Builder setAnimationStyle(int animstyle) {
            this.animstyle = animstyle;
            return this;
        }


        public CustomPopupWindow builder() {
            return new CustomPopupWindow(this);
        }
    }
}