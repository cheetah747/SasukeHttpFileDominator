package com.sibyl.HttpFileDominator.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * @author ...
 */
public class ZxingCodeTool {

    /**
     * 用字符串生成二维码
     *
     * @param str 要生成的字符串
     * @param w   宽，默认可写500
     * @param h   高，默认可写500
     * @return 二维码图片bitmap
     */
    public static Bitmap create2DCode(String str, int w, int h) {
        // 生成二维矩阵,编码时指定大小，不要生成了图片以后再进行缩放,这样会模糊导致识别失
        BitMatrix matrix;
        try {
            matrix = new MultiFormatWriter().encode(str, BarcodeFormat.QR_CODE, w, h);
        } catch (WriterException e) {

            return null;
        }
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        // 二维矩阵转为像素数组,也就是一直横
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (matrix.get(x, y)) {
                    pixels[y * width + x] = 0xff000000;
                }
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        // 通过像素数组生成bitmap,具体参api
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    /***
     * 给二维码添加白色背景
     * clipLength: 边缘裁剪长度
     */
    public static Bitmap setWhiteBackground(Bitmap bitmap,int clipLength) {
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        Bitmap blankBmp = Bitmap.createBitmap(bitmap.getWidth() - clipLength * 2,
                bitmap.getHeight() - clipLength * 2, bitmap.getConfig());
        Canvas canvas = new Canvas(blankBmp);
        canvas.drawRect(0, 0, blankBmp.getWidth(), blankBmp.getHeight(), paint);
        canvas.drawBitmap(bitmap, -clipLength, -clipLength, new Paint());
        return blankBmp;
    }


        /**
         * 黑点颜色
         */
    private static final int BLACK = 0xFF000000;
    /**
     * 白色
     */
    private static final int WHITE = 0xFFFFFFFF;
    /**
     * 正方形二维码宽度/高度
     */
    private static final int CODE_WIDTH = 500, CODE_HEIGHT = 500;
    /**
     * LOGO宽度值,最大不能大于二维码20%宽度值,大于可能会导致二维码信息失效
     */
    private static final int LOGO_WIDTH_MAX = CODE_WIDTH / 5;
    /**
     * LOGO宽度值,最小不能小于二维码10%宽度值,小于影响Logo与二维码的整体搭配
     */
    private static final int LOGO_WIDTH_MIN = CODE_WIDTH / 10;

    /**
     * 生成带LOGO的二维码
     */
    public static Bitmap create2DCodeWithLogo(String content, Bitmap logoBitmap) {
        try {
            int logoWidth = logoBitmap.getWidth();
            int logoHeight = logoBitmap.getHeight();
            int logoHaleWidth = logoWidth >= CODE_WIDTH ? LOGO_WIDTH_MIN
                    : LOGO_WIDTH_MAX;
            int logoHaleHeight = logoHeight >= CODE_HEIGHT ? LOGO_WIDTH_MIN
                    : LOGO_WIDTH_MAX;
            // 将logo图片按martix设置的信息缩放
            Matrix m = new Matrix();
        /*
         * 给的源码是,由于CSDN上传的资源不能改动，这里注意改一下
         * float sx = (float) 2*logoHaleWidth / logoWidth;
         * float sy = (float) 2*logoHaleHeight / logoHeight;
         */
            float sx = (float) logoHaleWidth / logoWidth;
            float sy = (float) logoHaleHeight / logoHeight;
            // 设置缩放信息
            m.setScale(sx, sy);
            Bitmap newLogoBitmap = Bitmap.createBitmap(logoBitmap, 0, 0, logoWidth,
                    logoHeight, m, false);
            int newLogoWidth = newLogoBitmap.getWidth();
            int newLogoHeight = newLogoBitmap.getHeight();
            Hashtable<EncodeHintType, Object> hints = new Hashtable<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);//设置容错级别,H为最高
//            hints.put(EncodeHintType.MAX_SIZE, LOGO_WIDTH_MAX);// 设置图片的最大值
//            hints.put(EncodeHintType.MIN_SIZE, LOGO_WIDTH_MIN);// 设置图片的最小值
//            hints.put(EncodeHintType.MARGIN, 2);//设置白色边距值
            // 生成二维矩阵,编码时指定大小,不要生成了图片以后再进行缩放,这样会模糊导致识别失败
            BitMatrix matrix = new MultiFormatWriter().encode(content,
                    BarcodeFormat.QR_CODE, CODE_WIDTH, CODE_HEIGHT, hints);
            int width = matrix.getWidth();
            int height = matrix.getHeight();
            int halfW = width / 2;
            int halfH = height / 2;
            // 二维矩阵转为一维像素数组,也就是一直横着排了
            int[] pixels = new int[width * height];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    /*
                     * 取值范围
                     * halfW + newLogoWidth / 2 - (halfW - newLogoWidth / 2) = newLogoWidth
                     * halfH + newLogoHeight / 2 - (halfH - newLogoHeight) = newLogoHeight
                     */
                    if (x > halfW - newLogoWidth / 2 && x < halfW + newLogoWidth / 2
                            && y > halfH - newLogoHeight / 2 && y < halfH + newLogoHeight / 2) {// 该位置用于存放图片信息
                        /*
                         *  记录图片每个像素信息
                         *  halfW - newLogoWidth / 2 < x < halfW + newLogoWidth / 2
                         *  --> 0 < x - halfW + newLogoWidth / 2 < newLogoWidth
                         *   halfH - newLogoHeight / 2  < y < halfH + newLogoHeight / 2
                         *   -->0 < y - halfH + newLogoHeight / 2 < newLogoHeight
                         *   刚好取值newLogoBitmap。getPixel(0-newLogoWidth,0-newLogoHeight);
                         */
                        pixels[y * width + x] = newLogoBitmap.getPixel(
                                x - halfW + newLogoWidth / 2, y - halfH + newLogoHeight / 2);
                    } else {
                        //此处可以修改二维码的颜色，可以分别制定二维码和背景的颜色；
                        pixels[y * width + x] = matrix.get(x, y) ? BLACK : WHITE;
                    }
                }
            }
            Bitmap bitmap = Bitmap.createBitmap(width, height,
                    Bitmap.Config.ARGB_8888);
            // 通过像素数组生成bitmap,具体参考api
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String decodeLocalQrCode(String path){
        return syncDecodeQRCode(getDecodeAbleBitmap(path));
    }

    /**
     * 同步解析bitmap二维码。该方法是耗时操作，请在子线程中调用。
     *
     * @param bitmap 要解析的二维码图片
     * @return 返回二维码图片里的内容 或 null
     */
    public static String syncDecodeQRCode(Bitmap bitmap) {
        Result result = null;
        RGBLuminanceSource source = null;

        List<BarcodeFormat> allFormats = new ArrayList<>();
        allFormats.add(BarcodeFormat.AZTEC);
        allFormats.add(BarcodeFormat.CODABAR);
        allFormats.add(BarcodeFormat.CODE_39);
        allFormats.add(BarcodeFormat.CODE_93);
        allFormats.add(BarcodeFormat.CODE_128);
        allFormats.add(BarcodeFormat.DATA_MATRIX);
        allFormats.add(BarcodeFormat.EAN_8);
        allFormats.add(BarcodeFormat.EAN_13);
        allFormats.add(BarcodeFormat.ITF);
        allFormats.add(BarcodeFormat.MAXICODE);
        allFormats.add(BarcodeFormat.PDF_417);
        allFormats.add(BarcodeFormat.QR_CODE);
        allFormats.add(BarcodeFormat.RSS_14);
        allFormats.add(BarcodeFormat.RSS_EXPANDED);
        allFormats.add(BarcodeFormat.UPC_A);
        allFormats.add(BarcodeFormat.UPC_E);
        allFormats.add(BarcodeFormat.UPC_EAN_EXTENSION);
        Map<DecodeHintType, Object> HINTS = new EnumMap<>(DecodeHintType.class);
        HINTS.put(DecodeHintType.TRY_HARDER, BarcodeFormat.QR_CODE);
        HINTS.put(DecodeHintType.POSSIBLE_FORMATS, allFormats);
        HINTS.put(DecodeHintType.CHARACTER_SET, "utf-8");

        try {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int[] pixels = new int[width * height];
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
            source = new RGBLuminanceSource(width, height, pixels);
            result = new MultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(source)), HINTS);
            return result.getText();
        } catch (Exception e) {
            e.printStackTrace();
            if (source != null) {
                try {
                    result = new MultiFormatReader().decode(new BinaryBitmap(new GlobalHistogramBinarizer(source)), HINTS);
                    return result.getText();
                } catch (Throwable e2) {
                    e2.printStackTrace();
                }
            }
            return null;
        }
    }
    /**
     * 将本地图片文件转换成可解码二维码的 Bitmap。为了避免图片太大，这里对图片进行了压缩。感谢 https://github.com/devilsen 提的 PR
     *
     * @param picturePath 本地图片文件路径
     * @return
     */
    private static Bitmap getDecodeAbleBitmap(String picturePath) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(picturePath, options);
            int sampleSize = options.outHeight / 400;
            if (sampleSize <= 0) {
                sampleSize = 1;
            }
            options.inSampleSize = sampleSize;
            options.inJustDecodeBounds = false;
            return BitmapFactory.decodeFile(picturePath, options);
        } catch (Exception e) {
            return null;
        }
    }
}