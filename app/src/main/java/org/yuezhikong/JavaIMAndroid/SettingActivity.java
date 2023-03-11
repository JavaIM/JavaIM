package org.yuezhikong.JavaIMAndroid;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.provider.Telephony;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.Nullable;

public class SettingActivity extends Activity {
    class OnClickSaveChange implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            OnSaveChange(v);
        }
    }
    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        //Super类调用
        super.onCreate(savedInstanceState);
        //设置显示界面
        setContentView(R.layout.setting_activity);
        //开始读取bundle，进行第一次填充
        Bundle bundle = this.getIntent().getExtras();
        int ServerPort = bundle.getInt("ServerPort");
        String ServerAddr = bundle.getString("ServerAddr");
        //进行第一次填充
        EditText AddrEdit = findViewById(R.id.SettingIPAddress);
        EditText PortEdit = findViewById(R.id.SettingIPPort);
        AddrEdit.setText(ServerAddr);
        if (ServerPort != 0) {
            PortEdit.setText(Integer.toString(ServerPort));
        }
        //填充完成
        //正在处理保存并退出按钮
        Button button = findViewById(R.id.button5);
        button.setOnClickListener(new OnClickSaveChange());
    }
    public void OnSaveChange(View view) {
        //开始获取新ServerAddr和新ServerPort
        EditText AddrEdit = findViewById(R.id.SettingIPAddress);
        EditText PortEdit = findViewById(R.id.SettingIPPort);
        //开始向bundle写入用户的新ServerAddr和新ServerPort
        MainActivity.ServerAddr = AddrEdit.getText().toString();
        try {
            MainActivity.ServerPort = Integer.parseInt(PortEdit.getText().toString());
        } catch (NumberFormatException e)
        {
            e.printStackTrace();
        }
        //退出此Activity
        this.finish();
    }
}
