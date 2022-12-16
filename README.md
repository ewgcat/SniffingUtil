# SniffingUtil
安卓视频嗅探
### 使用方式：
```

SniffingUtil.get()
.activity(MainActivity.this)
.connTimeOut(10000)
.readTimeOut(10000).
referer(url)
.url(url)
.callback(this)
.start();

```
### 回调
```

    @Override
    public void onSniffingSuccess(View webView, String url, List<SniffingVideo> videos) {
        LogUtil.e("MainActivity", "onSniffingSuccess videos " + videos.toString());
        tv.setText("嗅探地址: "+url+" \n 嗅探结果: "+videos.toString());
        dialog.dismiss();
    }

    @Override
    public void onSniffingStart(View webView, String url) {
        dialog.show();
    }

    @Override
    public void onSniffingError(View webView, String url, int errorCode) {
        LogUtil.e("MainActivity", "onSniffingError " );
        dialog.dismiss();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SniffingUtil.get().releaseAll();
    }
```
