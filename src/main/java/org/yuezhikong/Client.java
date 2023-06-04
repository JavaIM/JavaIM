package org.yuezhikong;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import com.google.gson.Gson;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.yuezhikong.utils.*;
import org.yuezhikong.utils.Protocol.LoginProtocol;
import org.yuezhikong.utils.Protocol.NormalProtocol;

import javax.crypto.SecretKey;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.Objects;
import java.util.UUID;

import static org.yuezhikong.CodeDynamicConfig.*;

public class Client {
    //public static final Logger logger = LogManager.getLogger(Client.class);
    protected Logger logger;
    protected String ServerPublicKey = null;
    protected Socket client;
    private final CustomVar.KeyData RSAKey;
    private cn.hutool.crypto.symmetric.AES AES;
    private static Client Instance;

    @Contract(pure = true)
    public static Client getInstance() {
        return Instance;
    }

    /**
     * Logger初始化
     */
    protected void LoggerInit()
    {
        logger = new Logger(false,false,null,null);
    }

    /**
     * 将要因为缺失key崩溃时调用
     */
    protected void PublicKeyLack()
    {
        Logger.logger_root.fatal("在运行目录下未找到ServerPublicKey.txt");
        Logger.logger_root.fatal("此文件为服务端公钥文件，用于保证通信安全");
        Logger.logger_root.fatal("由于此文件缺失，客户端即将停止运行");
        ExitSystem(-1);
    }

    /**
     * 因为连接被关闭导致退出调用此方法
     * 严禁合并，GUI客户端依赖于此方法
     */
    protected void ExitSystem(int code)
    {
        System.exit(0);
    }

    /**
     * 直接向服务端发送消息
     * @param input 消息
     * @return true为请退出程序，false为请继续执行
     * @throws IOException 出现io错误时
     */
    protected boolean SendMessageToServer(@NotNull String input) throws IOException
    {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
        if (About_System) {
            if (".about".equals(input)) {
                logger.info("JavaIM是根据GNU General Public License v3.0开源的自由程序（开源软件）");
                logger.info("主仓库位于：https://github.com/QiLechan/JavaIM");
                logger.info("主要开发者名单：");
                logger.info("QiLechan（柒楽）");
                logger.info("AlexLiuDev233 （阿白）");
                logger.info("仓库启用了不允许协作者直接推送到主分支，需审核后再提交");
                logger.info("因此，如果想要体验最新功能，请查看fork仓库，但不保证稳定性");
            }
        }
        if (".help".equals(input)) {
            logger.info("客户端命令列表");
            if (About_System) {
                logger.info(".about 查看本程序相关信息");
            }
            logger.info(".quit 断开与服务器的连接并终止本程序");
        }
        // 检查用户输入是否是.quit
        if (".quit".equals(input))
        {
            logger.info("正在断开连接");
            writer.write("quit\n");
            client.close();
            return true;
        }
        // 为控制台补上一个>
        System.out.print(">");
        // 将消息根据Protocol封装
        Gson gson = new Gson();
        NormalProtocol protocolData = new NormalProtocol();
        NormalProtocol.MessageHead MessageHead = new NormalProtocol.MessageHead();
        MessageHead.setVersion(CodeDynamicConfig.getProtocolVersion());
        MessageHead.setType("Chat");
        protocolData.setMessageHead(MessageHead);
        NormalProtocol.MessageBody MessageBody = new NormalProtocol.MessageBody();
        MessageBody.setFileLong(0);
        MessageBody.setMessage(input);
        protocolData.setMessageBody(MessageBody);
        input = gson.toJson(protocolData);
        // 加密信息
        input = java.net.URLEncoder.encode(input, StandardCharsets.UTF_8);
        if (GetRSA_Mode()) {
            if (isAES_Mode())
            {
                input = AES.encryptBase64(input,StandardCharsets.UTF_8);
            }
            else
                input = RSA.encrypt(input, ServerPublicKey);
        }
        // 发送消息给服务器
        writer.write(input);
        writer.newLine();
        writer.flush();
        return false;
    }
    private boolean getUserName;
    private boolean getPassword;
    /**
     * 循环检测控制台并将用户输入发信
     * @throws IOException 出现IO错误时抛出
     */
    protected void SendMessage() throws IOException {
        String UserName = "";
        while (true) {
            // 等待用户输入信息
            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
            String input = consoleReader.readLine();
            if (!(logger.isGUIMode())) {
                if (getUserName) {
                    logger.info("请输入密码");
                    UserName = input;
                    getUserName = false;
                    continue;
                }
                if (getPassword) {
                    getPassword = false;
                    LoginCallback(new CustomVar.UserAndPassword(UserName, input));
                }
            }
            if (SendMessageToServer(input))
            {
                break;
            }
        }
    }
    protected void GetUserNameAndUserPassword()
    {
        getUserName = true;
        getPassword = true;
        logger.info("请输入用户名");
    }
    protected void LoginCallback(CustomVar.@NotNull UserAndPassword userAndPassword)
    {
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
            Gson gson = new Gson();
            String data;
            LoginProtocol loginProtocol = new LoginProtocol();
            LoginProtocol.LoginPacketHeadBean LoginPacketHead = new LoginProtocol.LoginPacketHeadBean();
            LoginPacketHead.setType("passwd");
            loginProtocol.setLoginPacketHead(LoginPacketHead);
            LoginProtocol.LoginPacketBodyBean LoginPacketBody = new LoginProtocol.LoginPacketBodyBean();
            LoginProtocol.LoginPacketBodyBean.NormalLoginBean LoginBean = new LoginProtocol.LoginPacketBodyBean.NormalLoginBean();
            LoginBean.setUserName(userAndPassword.Username());
            LoginBean.setPasswd(userAndPassword.PassWord());
            LoginPacketBody.setNormalLogin(LoginBean);
            loginProtocol.setLoginPacketBody(LoginPacketBody);
            data = gson.toJson(loginProtocol);
            // 加密信息
            data = java.net.URLEncoder.encode(data, StandardCharsets.UTF_8);
            if (GetRSA_Mode()) {
                if (isAES_Mode()) {
                    data = AES.encryptBase64(data, StandardCharsets.UTF_8);
                } else
                    data = RSA.encrypt(data, ServerPublicKey);
            }
            // 发送消息给服务器
            writer.write(data);
            writer.newLine();
            writer.flush();
        } catch (IOException e)
        {
            SaveStackTrace.saveStackTrace(e);
            ExitSystem(0);
        }
    }
    public Client(String serverName, int port) {
        Instance = this;
        {
            LoggerInit();
            if (!(new File("ServerPublicKey.txt").exists()))
            {
                PublicKeyLack();
            }
        }
        RSAKey = RSA.generateKeyToReturn();
        Runnable recvmessage = () ->
        {
            PrivateKey privateKey = null;
            if (GetRSA_Mode()) {
                try {
                    privateKey = RSAKey.privateKey;
                } catch (Exception e) {
                    if (!"Socket closed".equals(e.getMessage()))
                    {
                        SaveStackTrace.saveStackTrace(e);
                    }
                    ExitSystem(-1);
                }
            }
            while (true)
            {
                BufferedReader reader;//获取输入流
                try {
                    reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    String msg = reader.readLine();
                    if (msg == null)
                    {
                        logger.info("连接早已被关闭...");
                        ExitSystem(0);
                        break;
                    }
                    if (GetRSA_Mode()) {
                        if (isAES_Mode())
                        {
                            msg = AES.decryptStr(msg);
                        }
                        else {
                            if (privateKey != null) {
                                msg = RSA.decrypt(msg, privateKey);
                            } else {
                                logger.error("错误，您的私钥为null，但现在处于RSA模式，无法解密此消息！");
                                continue;
                            }
                        }
                    }
                    msg = java.net.URLDecoder.decode(msg,StandardCharsets.UTF_8);
                    // 将信息从Protocol Json中取出
                    Gson gson = new Gson();
                    NormalProtocol protocolData = gson.fromJson(msg, NormalProtocol.class);
                    if (protocolData.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion())
                    {
                        logger.info("此服务器的协议版本与本客户端不相符，正在断开连接");
                        client.close();
                        break;
                    }
                    // type目前只实现了chat,FileTransfer延后
                    if (protocolData.getMessageHead().getType().equals("FileTransfer"))
                    {
                        logger.info("有人想要为您发送一个文件，但是此客户端暂不支持FileTransfer协议");
                    }
                    else if (!protocolData.getMessageHead().getType().equals("Chat"))
                    {
                        logger.warning("警告，服务端发来无法识别的非法数据包");
                    }
                    else {
                        msg = protocolData.getMessageBody().getMessage();
                        logger.ChatMsg(msg);
                    }
                }
                catch (IOException e)
                {
                    if (!"Connection reset by peer".equals(e.getMessage()) && !"Connection reset".equals(e.getMessage()) && !"Socket is closed".equals(e.getMessage()))  {
                        logger.warning("发生I/O错误");
                        SaveStackTrace.saveStackTrace(e);
                    }
                    else
                    {
                        logger.info("连接早已被关闭...");
                        ExitSystem(0);
                        break;
                    }
                }
            }
        };
        try {
            logger.info("连接到主机：" + serverName + " ，端口号：" + port);
            client = new Socket(serverName, port);
            logger.info("远程主机地址：" + client.getRemoteSocketAddress());
            OutputStream outToServer = client.getOutputStream();
            DataOutputStream out = new DataOutputStream(outToServer);
            InputStream inFromServer = client.getInputStream();
            DataInputStream in = new DataInputStream(inFromServer);
            if (GetRSA_Mode()) {
                ServerPublicKey = Objects.requireNonNull(RSA.loadPublicKeyFromFile("ServerPublicKey.txt")).PublicKey;
                String ClientRSAKey = java.net.URLEncoder.encode(Base64.encodeBase64String(RSAKey.publicKey.getEncoded()), StandardCharsets.UTF_8);
                out.writeUTF(ClientRSAKey);
                logger.info("服务器响应："+RSA.decrypt(in.readUTF(),RSAKey.privateKey));
                out.writeUTF(RSA.encrypt("Hello,Server! This Message By Client RSA System",ServerPublicKey));
                if (isAES_Mode())
                {
                    //客户端随机uuid
                    String RandomByClient = UUID.randomUUID().toString();
                    out.writeUTF(RSA.encrypt(java.net.URLEncoder.encode(RandomByClient, StandardCharsets.UTF_8),ServerPublicKey));
                    String RandomByServer = java.net.URLDecoder.decode(RSA.decrypt(in.readUTF(),RSAKey.privateKey),StandardCharsets.UTF_8);
                    byte[] KeyByte = new byte[32];
                    byte[] SrcByte = Base64.encodeBase64((RandomByClient+RandomByServer).getBytes(StandardCharsets.UTF_8));
                    System.arraycopy(SrcByte,0,KeyByte,0,31);
                    SecretKey key = SecureUtil.generateKey(SymmetricAlgorithm.AES.getValue(),KeyByte);
                    AES = cn.hutool.crypto.SecureUtil.aes(key.getEncoded());
                    logger.info("服务器响应："+AES.decryptStr(in.readUTF()));
                    out.writeUTF(AES.encryptBase64("Hello,Server! This Message By Client AES System"));
                }
                //out.writeUTF(RSA.encrypt(java.net.URLEncoder.encode("你", StandardCharsets.UTF_8),ServerPublicKey));
            }
            //后续握手过程还需测试RSA！
            out.writeUTF("Hello from " + client.getLocalSocketAddress());//通讯握手开始
            logger.info("服务器响应： " + in.readUTF());//通讯握手结束
            //握手（登录状态检测）
            {
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
                String data;
                Gson gson = new Gson();
                NormalProtocol protocolData = new NormalProtocol();
                NormalProtocol.MessageHead MessageHead = new NormalProtocol.MessageHead();
                MessageHead.setVersion(CodeDynamicConfig.getProtocolVersion());
                MessageHead.setType("Login");
                protocolData.setMessageHead(MessageHead);
                NormalProtocol.MessageBody MessageBody = new NormalProtocol.MessageBody();
                MessageBody.setFileLong(0);
                MessageBody.setMessage("Query");
                protocolData.setMessageBody(MessageBody);
                data = gson.toJson(protocolData);
                // 加密信息
                data = java.net.URLEncoder.encode(data, StandardCharsets.UTF_8);
                if (GetRSA_Mode()) {
                    if (isAES_Mode())
                    {
                        data = AES.encryptBase64(data,StandardCharsets.UTF_8);
                    }
                    else
                        data = RSA.encrypt(data, ServerPublicKey);
                }
                // 发送消息给服务器
                writer.write(data);
                writer.newLine();
                writer.flush();

                BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
                data = reader.readLine();
                if (data == null)
                {
                    logger.info("连接早已被关闭...");
                    ExitSystem(0);
                    System.exit(0);
                }
                if (GetRSA_Mode()) {
                    if (isAES_Mode())
                    {
                        data = AES.decryptStr(data);
                    }
                    else {
                        if (RSAKey.privateKey != null) {
                            data = RSA.decrypt(data, RSAKey.privateKey);
                        } else {
                            logger.error("错误，您的私钥为null，但现在处于RSA模式，无法解密此消息！");
                            ExitSystem(0);
                            System.exit(0);
                        }
                    }
                }
                data = java.net.URLDecoder.decode(data,StandardCharsets.UTF_8);
                // 将信息从Protocol Json中取出
                gson = new Gson();
                protocolData = gson.fromJson(data, NormalProtocol.class);
                if (protocolData.getMessageHead().getVersion() != CodeDynamicConfig.getProtocolVersion())
                {
                    logger.info("此服务器的协议版本与本客户端不相符，正在断开连接");
                    client.close();
                    ExitSystem(0);
                    System.exit(0);
                }
                // type目前只实现了chat,FileTransfer延后
                if (protocolData.getMessageHead().getType().equals("FileTransfer"))
                {
                    logger.info("有人想要为您发送一个文件，但是此客户端暂不支持FileTransfer协议");
                    client.close();
                    ExitSystem(0);
                    System.exit(0);
                }
                else if (!protocolData.getMessageHead().getType().equals("Login"))
                {
                    logger.warning("警告，服务端发来无法识别的非法数据包");
                    client.close();
                    ExitSystem(0);
                    System.exit(0);
                }
                else {
                    if ("Enable".equals(protocolData.getMessageBody().getMessage()))
                    {
                        if (new File("./token.txt").exists() && new File("./token.txt").isFile())
                        {
                            gson = new Gson();
                            LoginProtocol loginProtocol = new LoginProtocol();
                            LoginProtocol.LoginPacketHeadBean LoginPacketHead = new LoginProtocol.LoginPacketHeadBean();
                            LoginPacketHead.setType("Token");
                            loginProtocol.setLoginPacketHead(LoginPacketHead);
                            LoginProtocol.LoginPacketBodyBean LoginPacketBody = new LoginProtocol.LoginPacketBodyBean();
                            LoginProtocol.LoginPacketBodyBean.ReLoginBean TokenBean = new LoginProtocol.LoginPacketBodyBean.ReLoginBean();
                            TokenBean.setToken(FileUtils.readFileToString(new File("./token.txt"), StandardCharsets.UTF_8));
                            LoginPacketBody.setReLogin(TokenBean);
                            loginProtocol.setLoginPacketBody(LoginPacketBody);
                            data = gson.toJson(loginProtocol);
                            // 加密信息
                            data = java.net.URLEncoder.encode(data, StandardCharsets.UTF_8);
                            if (GetRSA_Mode()) {
                                if (isAES_Mode())
                                {
                                    data = AES.encryptBase64(data,StandardCharsets.UTF_8);
                                }
                                else
                                    data = RSA.encrypt(data, ServerPublicKey);
                            }
                            // 发送消息给服务器
                            writer.write(data);
                            writer.newLine();
                            writer.flush();
                        }
                        else {
                            GetUserNameAndUserPassword();
                        }
                    }
                    else {
                        UUID tmpUserName = UUID.randomUUID();
                        logger.info("您的用户名为："+ tmpUserName);

                        gson = new Gson();
                        protocolData = new NormalProtocol();
                        MessageHead = new NormalProtocol.MessageHead();
                        MessageHead.setVersion(CodeDynamicConfig.getProtocolVersion());
                        MessageHead.setType("Login");
                        protocolData.setMessageHead(MessageHead);
                        MessageBody = new NormalProtocol.MessageBody();
                        MessageBody.setFileLong(0);
                        MessageBody.setMessage(tmpUserName.toString());
                        protocolData.setMessageBody(MessageBody);
                        data = gson.toJson(protocolData);
                        // 加密信息
                        data = java.net.URLEncoder.encode(data, StandardCharsets.UTF_8);
                        if (GetRSA_Mode()) {
                            if (isAES_Mode())
                            {
                                data = AES.encryptBase64(data,StandardCharsets.UTF_8);
                            }
                            else
                                data = RSA.encrypt(data, ServerPublicKey);
                        }
                        // 发送消息给服务器
                        writer.write(data);
                        writer.newLine();
                        writer.flush();
                    }
                }
            }//握手结束
            Thread thread = new Thread(recvmessage);
            thread.start();
            thread.setName("RecvMessage Thread");
            //控制台输出检测
            SendMessage();
        }
        catch (IOException e)
        {
            if (!"Connection reset by peer".equals(e.getMessage()) && !"Connection reset".equals(e.getMessage())) {
                logger.warning("发生I/O错误");
                SaveStackTrace.saveStackTrace(e);
            }
            else
            {
                logger.info("连接早已被关闭...");
                ExitSystem(0);
            }
        } catch (Exception e) {
            logger.error("由于某些错误，我们无法发送您的信息");
            logger.error("报错信息："+e.getMessage());
        }
    }
}
