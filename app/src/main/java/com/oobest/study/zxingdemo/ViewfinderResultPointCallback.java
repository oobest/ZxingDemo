package com.oobest.study.zxingdemo;

import com.google.zxing.ResultPoint;
import com.google.zxing.ResultPointCallback;

/**
 * Created by oujianfeng on 2017/4/25.
 */

public class ViewfinderResultPointCallback implements ResultPointCallback {

    private final ViewfinderView viewfinderView;

    ViewfinderResultPointCallback(ViewfinderView viewfinderView) {
        this.viewfinderView = viewfinderView;
    }

    @Override
    public void foundPossibleResultPoint(ResultPoint point) {
        viewfinderView.addPossibleResultPoint(point);
    }
}
