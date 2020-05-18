package com.demo.autostitchscreenshot.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

public class Utils {
    public static String getRealImagePathFromURI(Uri contentUri, Context context) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = context.getContentResolver().query(contentUri, projection, null, null, null);
        String realPath = "";
        if(cursor!=null && cursor.moveToFirst()){
            int colIndex = cursor.getColumnIndex(projection[0]);
            realPath = cursor.getString(colIndex);
            cursor.close();
        }
        return realPath;
    }
}
