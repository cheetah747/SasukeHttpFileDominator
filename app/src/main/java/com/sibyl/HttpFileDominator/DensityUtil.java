package com.sibyl.HttpFileDominator;

import android.content.res.Resources;
import android.util.TypedValue;

/**
 * @author Sasuke on 2020/5/31.
 */

public class DensityUtil {
    public static int dp2px(float dpValue) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,dpValue, Resources.getSystem().getDisplayMetrics());
    }
}
