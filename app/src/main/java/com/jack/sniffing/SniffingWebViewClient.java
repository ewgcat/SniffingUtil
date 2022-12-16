package com.jack.sniffing;


import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * SniffingWebViewClient
 */
public class SniffingWebViewClient extends WebViewClient implements SniffingCallback {
    public static final int READ_TIME_OUT = 1;
    public static final int RECEIVED_ERROR = 2;
    public static final int NOT_FIND = 3;
    public static final int CONNECTION_ERROR = 4;

    public static final int TYPE_CONN = 0;
    public static final int TYPE_READ = 1;
    ArrayList<SniffingVideo> mVideos=new ArrayList<>();
    private boolean isCompleteLoader = true;

    private Handler mH = new Handler(Looper.getMainLooper());

    private Map<String, String> mHeader;
    private DefaultFilter mFilter;
    private SniffingCallback mCallback;

    private long mLastStartTime;
    private long mLastEndTime = System.currentTimeMillis();

    private TimeOutRunnable mConnTimeoutRunnable = null;
    private TimeOutRunnable mReadTimeoutRunnable = null;
    private long mConnTimeOut = 20 * 1000;
    private long mReadTimeOut = 45 * 1000;
    private long mFinishedTimeOut = 800;
    private WebView mWebView;
    private String mURL;


    public SniffingWebViewClient(WebView mWebView, String mURL, Map<String, String> mHeader,
                                 DefaultFilter mFilter, SniffingCallback mCallback) {
        this.mHeader = mHeader;
        this.mWebView = mWebView;
        this.mURL = mURL;
        this.mFilter = mFilter;
        this.mCallback = mCallback;
    }


    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (url.startsWith("http"))
            view.loadUrl(url, mHeader);
        return true;
    }

    public void setConnTimeOut(long connTimeOut) {
        this.mConnTimeOut = connTimeOut;
    }

    public void setFinishedTimeOut(long mFinishedTimeOut) {
        this.mFinishedTimeOut = mFinishedTimeOut;
    }

    public void setReadTimeOut(long readTimeOut) {
        this.mReadTimeOut = readTimeOut;
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        if (mLastEndTime - mLastStartTime <= 500 || !isCompleteLoader) { // 基本上是302 重定向才会走这段逻辑
            LogUtil.e("SniffingUtil", "onStart( 302 )  --> " + url);
            return;
        }
        if (this.mConnTimeoutRunnable != null) {
            this.mH.removeCallbacks(this.mConnTimeoutRunnable);
        }
        this.mH.postDelayed(this.mConnTimeoutRunnable = new TimeOutRunnable(view, url, TYPE_CONN), mConnTimeOut);
        LogUtil.e("SniffingUtil", "onStart(onPageStarted)  --> " + url);
        this.onSniffingStart(view, url);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        mLastEndTime = System.currentTimeMillis();
    }

    @Override
    //为了headers能够正确写入，建议使用Request的拦截
    public WebResourceResponse shouldInterceptRequest(WebView webView, WebResourceRequest webResourceRequest) {
        try {
            //播放地址
            String url = webResourceRequest.getUrl().toString();
            //请求头
            Map<String, String> headers = webResourceRequest.getRequestHeaders();
            //过滤器过滤
            mFilter.addHeaders(headers);
            //得到视频本体
            SniffingVideo video =  mFilter.onFilter(webView, url);
            if (video!=null){
                Log.d("sniffed video:", video.getUrl());
//                boolean hasReferer = headers.containsKey("Referer");
//                boolean hasOrigin = headers.containsKey("Origin");
//                HttpReferer referer = HttpReferer.getInstance(video.getUrl(), video.getUrl());
//                Map<String, String> maps = referer.getMap();
//                if ((hasReferer || hasOrigin) && maps.size() == 0){
//                    //一般保留Referer和Origin就能保证视频播放
//                    if (hasReferer && hasOrigin) {
//                        maps = new HashMap<>(2);
//                        maps.put("Referer", headers.get("Referer"));
//                        maps.put("Origin", headers.get("Origin"));
//                    }else {
//                        maps = new HashMap<>(1);
//                        if (hasReferer)
//                            maps.put("Referer", headers.get("Referer"));
//                        else
//                            maps.put("Origin", headers.get("Origin"));
//                    }
//                    video.addHeaders(maps);
//                }
                mVideos.add(video);
                SniffingWebViewClient.this.onSniffingSuccess(webView, url, mVideos);
                //如果需要禁止视频的加载，就返回空请求（建议）
                return new WebResourceResponse(null, null, null);
            }
            //信息不完整，只知道是视频，不知道类型
        } catch (Throwable e) {
            e.printStackTrace();
        }

        return super.shouldInterceptRequest(webView, webResourceRequest);
    }


    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        if (mVideos!=null&&!mVideos.isEmpty()) {
            LogUtil.e("SniffingUtil", "onReceivedError(SUCCESS)  --> " + failingUrl);
            SniffingWebViewClient.this.onSniffingSuccess(view, failingUrl, mVideos);
        } else {
            LogUtil.e("SniffingUtil", "onReceivedError(ReceivedError)  --> " + failingUrl);
            this.onSniffingError(view, failingUrl, RECEIVED_ERROR);
        }
    }

    @Override
    public void onReceivedSslError(WebView webView, SslErrorHandler sslErrorHandler, SslError sslError) {
        sslErrorHandler.proceed();
    }

    @Override
    public void onSniffingStart(final View webView, final String url) {
        this.isCompleteLoader = false;
        this.mLastStartTime = System.currentTimeMillis();
        if (this.mReadTimeoutRunnable != null) {
            this.mH.removeCallbacks(this.mReadTimeoutRunnable);
        }
        this.mH.postDelayed(this.mReadTimeoutRunnable = new TimeOutRunnable((WebView) webView, url, TYPE_READ), mReadTimeOut);
        if (this.mCallback != null) {
            this.mH.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onSniffingStart(webView, url);
                }
            });
        }
    }

    @Override
    public void onSniffingSuccess(final View webView, final String url, final List<SniffingVideo> videos) {
        this.isCompleteLoader = true;
        this.mH.removeCallbacks(mReadTimeoutRunnable);
        this.mReadTimeoutRunnable = null;
        if (this.mCallback != null) {
            this.mH.post(new Runnable() {
                @Override
                public void run() {
                   if(mVideos!=null&&!mVideos.isEmpty()){
                       mCallback.onSniffingSuccess(webView, url, mVideos);
                   }
                }

            });
        }
    }

    @Override
    public void onSniffingError(final View webView, final String url, final int errorCode) {
        this.isCompleteLoader = true;
        this.mH.removeCallbacks(mReadTimeoutRunnable);
        this.mReadTimeoutRunnable = null;
        if (this.mCallback != null) {
            this.mH.post(new Runnable() {

                @Override
                public void run() {
                    mCallback.onSniffingError(webView, url, errorCode);
                }

            });
        }
    }



    //一次网页加载，解析超时
    private class TimeOutRunnable implements Runnable {

        private WebView view;
        private String url;
        private int type;

        public TimeOutRunnable(WebView view, String url, int type) {
            this.view = view;
            this.url = url;
            this.type = type;
        }

        @Override
        public void run() {
            //加载网页超时了
            if (type == TYPE_CONN) {
                if (mConnTimeoutRunnable == null) return;
                mH.removeCallbacks(mConnTimeoutRunnable);
                mConnTimeoutRunnable = null;
                if (mVideos!=null&&!mVideos.isEmpty()) {
                    LogUtil.e("SniffingUtil", "ConnTimeOutRunnable(SUCCESS)  --> " + url);
                } else {
                    LogUtil.e("SniffingUtil", "ConnTimeOutRunnable  --> " + url);
                    SniffingWebViewClient.this.onSniffingError(view, url, CONNECTION_ERROR);
                }
            } else if (type == TYPE_READ) {
                if (mVideos!=null&&!mVideos.isEmpty()) {
                    LogUtil.e("SniffingUtil", "ReadTimeOutRunnable(SUCCESS)  --> " + url);
                } else {
                    LogUtil.e("SniffingUtil", "ReadTimeOutRunnable  --> " + url);
                    SniffingWebViewClient.this.onSniffingError(view, url, READ_TIME_OUT);
                }
            }
        }

    }

//    private class ConnectionThread extends Thread {
//
//        private String url;
//        private String type;
//        private WebView view;
//
//        public ConnectionThread(WebView view, String url, String type) {
//            this.view = view;
//            this.url = url;
//            this.type = type;
//        }
//
//        @Override
//        public void run() {
//            try {
//                Object[] content = Util.getContent(url);
//                Object contentType = content[1];
//                if (contentType == null) {
//                    LogUtil.e("SniffingUtil", "onError(contentType == null)  --> " + url);
//                    SniffingWebViewClient.this.onSniffingError(view, url, CONTENT_ERROR);
//                } else if (contentType.toString().contains("html")) {
//                    LogUtil.e("SniffingUtil", "RELOAD()  --> " + url);
//                    if (mConnTimeoutRunnable != null) {
//                        mH.removeCallbacks(mConnTimeoutRunnable);
//                    }
//                    mH.postDelayed(mConnTimeoutRunnable = new TimeOutRunnable(view, url, TYPE_CONN), mConnTimeOut);
//                    mHeader.put("Referer", mWebView.getUrl());
//                    mWebView.loadUrl(Util.warpUrl(mURL, url), mHeader);
//                } else if (contentType.toString().contains("video") || contentType.toString().contains("mpegurl")) {
//                    LogUtil.e("SniffingUtil", "onSuccess(mpegurl video)  --> " + url);
//                    SniffingUtil.addSniffingVideoList(mURL,new SniffingVideo(url, type, (int) content[0], contentType.toString()));
//                    ArrayList<SniffingVideo> mVideos = SniffingUtil.getSniffingVideoList(mURL);
//                    SniffingWebViewClient.this.onSniffingSuccess(view, url, mVideos);
//                }
//            } catch (Throwable e) {
//                LogUtil.e("SniffingUtil", "onError(Throwable)  --> " + url);
//                SniffingWebViewClient.this.onSniffingError(view, url, CONNECTION_ERROR);
//            }
//        }
//
//    }

}
