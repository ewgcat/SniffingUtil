package com.jack.sniffing;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,SniffingCallback {

    private EditText editText;
    private Button bt;
    private TextView tv;
    private ProgressDialog dialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        editText = (EditText)findViewById(R.id.editText);
        bt = (Button)findViewById(R.id.bt);
        tv = (TextView)findViewById(R.id.tv);
        editText.setText("https://ukzy.ukubf3.com/share/6pYz58fgttoCxhus");
        dialog = new ProgressDialog(this);
        dialog.setTitle("正在嗅探....");
        bt.setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        String url=editText.getText().toString();
        if(!TextUtils.isEmpty(url)){
            SniffingUtil.get().activity(MainActivity.this).connTimeOut(10000).readTimeOut(10000).referer(url).url(url).callback(this).start();
        }
    }

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

}