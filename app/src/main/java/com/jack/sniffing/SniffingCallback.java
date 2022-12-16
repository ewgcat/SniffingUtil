package com.jack.sniffing;

import android.view.View;

import java.util.List;

public interface SniffingCallback {

    /**
     * 视频嗅探成功
     * @param webView
     * @param url
     * @param videos
     */
    void onSniffingSuccess(View webView,String url,List<SniffingVideo> videos);

    /**
     * 开始视频嗅探
     * @param webView
     * @param url
     */
    void onSniffingStart(View webView,String url);


    /**
     * 视频嗅探失败成功
     * @param webView
     * @param url
     * @param errorCode
     */
    void onSniffingError(View webView, String url, int errorCode);

}
