package org.yuezhikong.JavaIMAndroid;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.util.Base64;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.security.PrivateKey;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private KeyData RSAKey;
    private boolean Session = false;
    private Socket socket;
    public static String ServerAddr = "";
    public static int ServerPort = 0;
    class GoToSettingActivityListen implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            ChangeToSettingActivity(v);
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView ChatLog = findViewById(R.id.ChatLog);
        ChatLog.setMovementMethod(ScrollingMovementMethod.getInstance());
        Button GoToSettingActivityButton = findViewById(R.id.button4);
        GoToSettingActivityButton.setOnClickListener(new GoToSettingActivityListen());
        requestPermission();
    }
    @SuppressLint("SetTextI18n")
    public void Connect(View view) {
        ((TextView)findViewById(R.id.Error)).setText("");
        if (socket == null)
        {
            Session = false;
        }
        else {
            if (socket.isClosed()) {
                Session = false;
            }
        }
        String IPAddress = ServerAddr;
        if (IPAddress.isEmpty())
        {
            TextView ErrorOutput = findViewById(R.id.Error);
            ErrorOutput.setText(R.string.Error1);
        }
        int port = ServerPort;
        if (port <= 0)
        {
            TextView ErrorOutput = findViewById(R.id.Error);
            ErrorOutput.setText(R.string.Error2);
        }
        if (port > 65535)
        {
            TextView ErrorOutput = findViewById(R.id.Error);
            ErrorOutput.setText(R.string.Error2);
        }
        if (!Session)
        {
            ((TextView)findViewById(R.id.ChatLog)).setText("");
            Session = true;
            Runnable NetworkThread = () ->
            {
                try {
                    // 创建Socket对象 & 指定服务端的IP 及 端口号
                    socket = new Socket(IPAddress, port);
                    //获取文件
                    /*
                    File Storage = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/JavaIMFiles");
                    if (!Storage.exists()) {
                        Storage.mkdir();
                    }
                    File ServerPublicKeyFile = new File(Storage.getAbsolutePath() + "/ServerPublicKey.key");
                    if (!ServerPublicKeyFile.exists()) {
                        runOnUiThread(()->{
                            TextView ErrorOutput = findViewById(R.id.Error);
                            ErrorOutput.setText(R.string.Error5);
                        });
                        socket.close();
                        Session = false;
                    }
                     */
                    RSAKey = RSA.generateKeyToReturn();
                    @SuppressLint("SetTextI18n")
                    Runnable recvmessage = () ->
                    {
                        PrivateKey privateKey;
                        try {
                            privateKey = RSAKey.privateKey;
                        } catch (Exception e) {
                            Session = false;
                            return;
                        }
                        while (true) {
                            BufferedReader reader;//获取输入流
                            try {
                                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                                String msg = reader.readLine();
                                if (msg == null) {
                                    break;
                                }
                                if (privateKey != null) {
                                    msg = RSA.decrypt(msg, privateKey);
                                }
                                msg = java.net.URLDecoder.decode(msg, "UTF-8");
                                String finalMsg = msg;
                                runOnUiThread(
                                        () -> {
                                            TextView SocketOutput = findViewById(R.id.ChatLog);
                                            SocketOutput.setText(SocketOutput.getText() + finalMsg + "\r\n");
                                        }
                                );
                            } catch (IOException e) {
                                if (!"Connection reset by peer".equals(e.getMessage()) && !"Connection reset".equals(e.getMessage()) && !"Socket is closed".equals(e.getMessage())) {
                                    e.printStackTrace();
                                } else {
                                    runOnUiThread(
                                            () -> {
                                                TextView SocketOutput = findViewById(R.id.ChatLog);
                                                SocketOutput.setText(SocketOutput.getText() + "连接早已被关闭");
                                                Session = false;
                                            }
                                    );
                                    break;
                                }
                            }
                        }
                    };
                    TextView SocketOutput = findViewById(R.id.ChatLog);
                    runOnUiThread(()->{
                        SocketOutput.setText(SocketOutput.getText() + "连接到主机：" + IPAddress + " ，端口号：" + port + "\r\n");
                        SocketOutput.setText(SocketOutput.getText() + socket.getRemoteSocketAddress().toString() + "\r\n");
                    });
                    OutputStream outToServer = socket.getOutputStream();
                    DataOutputStream out = new DataOutputStream(outToServer);
                    String ClientRSAKey = java.net.URLEncoder.encode(Base64.encodeToString(RSAKey.publicKey.getEncoded(),Base64.NO_WRAP), "UTF-8");
                    out.writeUTF(ClientRSAKey);
                    out.writeUTF("Hello from " + socket.getLocalSocketAddress());//通讯握手开始
                    InputStream inFromServer = socket.getInputStream();
                    DataInputStream in = new DataInputStream(inFromServer);
                    String Message = in.readUTF();
                    runOnUiThread(()->{
                        SocketOutput.setText(SocketOutput.getText() + "服务器响应： " + Message + "\r\n");
                    });
                    Thread thread = new Thread(recvmessage);
                    thread.start();
                    thread.setName("RecvMessage Thread");


                } catch (IOException e) {
                    runOnUiThread(()->{
                        TextView ErrorOutput = findViewById(R.id.Error);
                        ErrorOutput.setText(R.string.Error3);
                    });
                }
            };
            Thread NetworKThread = new Thread(NetworkThread);
            NetworKThread.start();
            NetworKThread.setName("Network Thread");
        }
    }
    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 先判断有没有权限
            if (!(ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1024);
            }
        }
    }

    //用户按下发送按钮
    public void Send(View view) {
        ((TextView)findViewById(R.id.Error)).setText("");
        if (socket == null)
        {
            Session = false;
        }
        else {
            if (socket.isClosed()) {
                Session = false;
            }
        }
        EditText UserMessageText = findViewById (R.id.UserSendMessage);
        String UserMessage = UserMessageText.getText().toString();
        if (!Session)
        {
            TextView ErrorOutput = findViewById(R.id.Error);
            ErrorOutput.setText(R.string.Error6);
        }
        else
        {
            if (socket == null)
            {
                Session = false;
                return;
            }
            final String UserMessageFinal = UserMessage;
            Runnable NetworkThreadRunnable = ()->{
                BufferedWriter writer = null;
                try {
                    writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (".about".equals(input))
                {
                    TextView SocketOutput = findViewById(R.id.ChatLog);
                    runOnUiThread(()->{
                        SocketOutput.setText(SocketOutput.getText() + "JavaIM是根据GNU General Public License v3.0开源的自由程序（开源软件）\r\n");
                        SocketOutput.setText(SocketOutput.getText() + "主仓库位于：https://github.com/QiLechan/JavaIM\r\n");
                        SocketOutput.setText(SocketOutput.getText() + "主要开发者名单：\r\n");
                        SocketOutput.setText(SocketOutput.getText() + "QiLechan（柒楽）\r\n");
                        SocketOutput.setText(SocketOutput.getText() + "AlexLiuDev233 （阿白）\r\n");
                        SocketOutput.setText(SocketOutput.getText() + "仓库启用了不允许协作者直接推送到主分支，需审核后再提交\r\n");
                        SocketOutput.setText(SocketOutput.getText() + "因此，如果想要体验最新功能，请查看fork仓库，但不保证稳定性\r\n");
                    });
                }
                // 加密信息
                String input = UserMessageFinal;
                try {
                    input = java.net.URLEncoder.encode(input, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                //获取文件
                File Storage = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/JavaIMFiles");
                if (!Storage.exists()) {
                    Storage.mkdir();
                }
                File ServerPublicKeyFile = new File(Storage.getAbsolutePath() + "/ServerPublicKey.key");
                if (!ServerPublicKeyFile.exists()) {
                    runOnUiThread(()->{
                        TextView ErrorOutput = findViewById(R.id.Error);
                        ErrorOutput.setText(R.string.Error5);
                    });
                    return;
                }
                String ServerPublicKey = Objects.requireNonNull(RSA.loadPublicKeyFromFile(ServerPublicKeyFile.getAbsolutePath())).PublicKey;
                input = RSA.encrypt(input, ServerPublicKey);
                // 发送消息给服务器
                try {
                    Objects.requireNonNull(writer).write(input);
                    writer.newLine();
                    writer.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            };
            Thread NetWorkThread = new Thread(NetworkThreadRunnable);
            NetWorkThread.start();
            NetWorkThread.setName("Network Thread");
        }
    }

    public void Disconnect(View view) {
        ((TextView)findViewById(R.id.Error)).setText("");
        if (socket == null)
        {
            Session = false;
        }
        else {
            if (socket.isClosed()) {
                Session = false;
            }
        }
        if (Session)
        {
            Session = false;
            Runnable NetworkThread = () ->
            {
                try {
                    BufferedWriter writer = null;
                    try {
                        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (writer == null)
                    {
                        socket.close();
                        return;
                    }
                    writer.write("quit\n");
                    writer.newLine();
                    writer.flush();
                    writer.close();
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            };
            Thread NetworKThread = new Thread(NetworkThread);
            NetworKThread.start();
            NetworKThread.setName("Network Thread");
        }
    }
    public void ChangeToSettingActivity(View view) {
        String tmpServerAddr;
        int tmpServerPort;
        //处理当前已经记录的Addr和Port
        if (ServerAddr == null)
        {
            tmpServerAddr = "";
        }
        else
        {
            tmpServerAddr = ServerAddr;
        }
        tmpServerPort = ServerPort;
        //开始创建新Activity过程
        Intent intent=new Intent();
        intent.setClass(MainActivity.this, SettingActivity.class);
        //开始向新Activity发送老Addr和Port，以便填充到编辑框
        Bundle bundle = new Bundle();
        bundle.putString("ServerAddr",tmpServerAddr);
        bundle.putInt("ServerPort",tmpServerPort);
        //从Bundle put到intent
        intent.putExtras(bundle);
        //设置 如果这个activity已经启动了，就不产生新的activity，而只是把这个activity实例加到栈顶
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        //启动Activity
        startActivity(intent);
    }

}