package com.sibyl.HttpFileDominator.utils;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

/**
 * @author Sasuke on 2020/6/8.
 */

public class FuckGoogleUtil {
    public static String getUriPath(ContentResolver contentResolver, Uri uri) {
        if (uri.getPath().startsWith("file://")){
            return null;
        }
        String path = null;
        Cursor cursor = contentResolver.query(uri, null, null, null, null);
        if (cursor == null) {
            return null;
        }
        if (cursor.moveToFirst()) {
            try {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        cursor.close();
        return path;
    }
}
