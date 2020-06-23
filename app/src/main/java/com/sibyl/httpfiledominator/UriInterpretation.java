/*
 * Contatins code from https://github.com/k9mail/k-9/blob/master/src/com/fsck/k9/activity/MessageCompose.java
 * APACHE 2.0 License.
 *
 */
package com.sibyl.httpfiledominator;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.annotation.Nullable;

import com.sibyl.httpfiledominator.utils.FuckGoogleUtil;
import com.sibyl.httpfiledominator.utils.Util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URLDecoder;

public class UriInterpretation {

    private long size = -1;
    private String name = null;
    private String path = null;
    private String mime;
    private boolean isDirectory = false;
    public boolean isClipboardType = false;
    private Uri uri;
    private ContentResolver contentResolver;
    public String clipboardText = "";

    public InputStream getInputStream() throws FileNotFoundException {
        return contentResolver.openInputStream(uri);
    }

    public UriInterpretation(Uri uri, String clipboardText, ContentResolver contentResolver) {
        this(uri,contentResolver);
        this.isClipboardType = true;
        this.clipboardText = clipboardText;
    }


    public UriInterpretation(Uri uri, ContentResolver contentResolver) {
        this.uri = uri;

        this.contentResolver = contentResolver;

        Cursor metadataCursor = contentResolver.query(uri, new String[]{
                        OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}, null,
                null, null);

        if (metadataCursor != null) {
            try {
                if (metadataCursor.moveToFirst()) {
                    path = name = metadataCursor.getString(0);
                    size = metadataCursor.getInt(1);
                }
            } finally {
                metadataCursor.close();
            }
        }

        //解决：单独再处理一下获取文件大小（尽可能不要用上面的query，可能媒体数据库的信息还未更新，导致获取到的文件size有误，会导致文件下载下来不完整）
        try {
            ParcelFileDescriptor fd = contentResolver.openFileDescriptor(uri, "r");
            if (fd != null){
                size = fd.getStatSize();//纠正！
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        if (name == null) {
            name = uri.getLastPathSegment();
            path = uri.toString();
        }

        getMime(uri, contentResolver);

        getFileSize(uri);

    }

    private void getFileSize(Uri uri) {
        String realPath = FuckGoogleUtil.getUriPath(contentResolver,uri);
        if (size <= 0) {
            String uriString = uri.toString();
            // file://的情况======================
            if (uriString.startsWith("file://")) {
                File f = new File(uriString.substring("file://".length()));
                isDirectory = f.isDirectory();
                if (isDirectory) {
                    // Log.v(Util.myLogName, "We are dealing with a directory.");
                    size = 0;
                    return;
                }
                size = f.length();
                if (size == 0) {
                    uriString = URLDecoder.decode(uriString).substring(
                            "file://".length());
                    f = new File(uriString);
                    size = f.length();
                }
                ///Log.v(Util.myLogName, "zzz" + size);

            }
            //安卓7以上的content://的情况======================
            else if (uriString.startsWith("content://")){
                File f = new File(realPath);
                isDirectory = f.isDirectory();
                return;
            //其它的普通情况======================
            }else {
                try {
                    File f = new File(uriString);
                    isDirectory = f.isDirectory();
                    return;
                } catch (Exception e) {
                    Log.v(Util.myLogName, "Not a file... " + uriString);
                    e.printStackTrace();
                }
                Log.v(Util.myLogName, "Not a file: " + uriString);

            }
        }
    }

    private void getMime(Uri uri, ContentResolver contentResolver) {
        mime = contentResolver.getType(uri);
        if (mime == null || name == null) {
            mime = "application/octet-stream";
            if (name == null) {
                return;
            }
        }
        if (mime.equals("application/octet-stream")) {
            // we can do better than that
            int pos = name.lastIndexOf('.');
            if (pos < 0)
                return;
            String extension = name.substring(pos).toLowerCase();
            if (extension.equals(".jpg")) {
                mime = "image/jpeg";
                return;
            }
            if (extension.equals(".png")) {
                mime = "image/png";
                return;
            }
            if (extension.equals(".gif")) {
                mime = "image/gif";
                return;
            }
            if (extension.equals(".icon")) {
                mime = "image/x-icon";
                return;
            }
            if (extension.equals(".bmp")) {
                mime = "image/bmp";
                return;
            }
            if (extension.equals(".webp")) {
                mime = "image/webp";
                return;
            }
            if (extension.equals(".mp4")) {
                mime = "video/mp4";
                return;
            }
            if (extension.equals(".avi")) {
                mime = "video/avi";
                return;
            }
            if (extension.equals(".mp3")) {
                mime = "audio/x-mpeg-3";
                return;
            }
            if (extension.equals(".wav")) {
                mime = "audio/x-wav";
                return;
            }

            if (extension.equals(".mov")) {
                mime = "video/mov";
                return;
            }
            if (extension.equals(".vcf")) {
                mime = "text/x-vcard";
                return;
            }
            if (extension.equals(".txt")) {
                mime = "text/plain; charset=utf-8";
                return;
            }
            if (extension.equals(".html")) {
                mime = "text/html";
                return;
            }
            if (extension.equals(".json")) {
                mime = "application/json";
                return;
            }
            if (extension.equals(".epub")) {
                mime = "application/epub+zip";
                return;
            }
            if (extension.equals(".pdf")) {
                mime = "application/pdf";
                return;
            }
            if (extension.equals(".doc")) {
                mime = "application/msword";
                return;
            }
            if (extension.equals(".ppt")) {
                mime = "application/vnd.ms-powerpoint";
                return;
            }
            if (extension.equals(".xls")) {
                mime = "application/vnd.ms-excel";
                return;
            }

            //好像并没有用============
            if (extension.equals(".flv")) {
                mime = "video/x-flv";
                return;
            }
            if (extension.equals(".f4v")) {
                mime = "video/x-f4v";
                return;
            }
            if (extension.equals(".mkv")) {
                mime = "video/webm";
                return;
            }
            if (extension.equals(".docx")) {
                mime = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                return;
            }
            if (extension.equals(".pptx")) {
                mime = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
                return;
            }

            if (extension.equals(".xlsx")) {
                mime = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                return;
            }
            //好像并没有用============
        }
    }

    /**判断该文件类型是否是视频*/
    public static boolean isMimeVideoType(String mime){
        return mime.equals("video/mp4")
                || mime.equals("video/avi")
                || mime.equals("audio/x-mpeg-3")
                || mime.equals("audio/x-wav")
                || mime.equals("video/mov")
                || mime.equals("video/x-flv")
                || mime.equals("video/x-f4v")
                || mime.equals("video/webm");
    }

    public long getSize() {
        return size;
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        return name;
    }

    public String getMime() {
        return mime;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public Uri getUri() {
        return uri;
    }

    @Override
    public int hashCode() {
//		return super.hashCode();
        return uri.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
//		return super.equals(obj);
        if (obj instanceof UriInterpretation) {
            return ((UriInterpretation) obj).getUri().equals(this.getUri());
        }
        return super.equals(obj);
    }
}
