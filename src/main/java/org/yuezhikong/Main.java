package org.yuezhikong;

//import org.apache.logging.log4j.LogManager;
import org.yuezhikong.Server.Server;
import org.yuezhikong.utils.SaveStackTrace;

import java.io.*;
import java.util.Scanner;

import static org.yuezhikong.CodeDynamicConfig.GetAutoSaveDependencyMode;

public class Main {
    private static final org.yuezhikong.utils.Logger logger = new org.yuezhikong.utils.Logger();
    private static Main instance;
    public static Main getInstance()
    {
        if (instance == null)
        {
            instance = new Main();
        }
        return instance;
    }
    /**
     * @apiNote 从jar中释放文件到jar文件夹下的某一子文件夹
     * @param name 需要从jar中释放的文件在jar中的路径路径
     * @param dir 需要释放到的文件夹
     */
    public void saveJarFiles(String name,String dir) {
        File JarOnDir = new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().getFile()).getParentFile();
        File LibsDir = new File(JarOnDir.getPath()+dir);
        if (!LibsDir.exists()) {// 父目录不存在时先创建
            try {
                if (!LibsDir.mkdirs())
                {
                    System.err.println("创建文件夹失败");
                    System.exit(-1);
                }
            }
            catch (Exception e)
            {
                SaveStackTrace.saveStackTrace(e);
                System.exit(-1);
            }
        }
        File CheckFile = new File(LibsDir.getPath()+name);
        if (!CheckFile.exists())
        {
            try {
                if (!CheckFile.createNewFile())
                {
                    System.err.println("创建文件失败");
                    System.exit(-1);
                }
            }
            catch (Exception e)
            {
                System.err.println("创建文件时出现异常");
                SaveStackTrace.saveStackTrace(e);
                System.exit(-1);
            }
            InputStream inputStream = this.getClass().getResourceAsStream(name);
            try {
                assert inputStream != null;
                OutputStream os;
                os = new FileOutputStream(CheckFile);
                int index;
                byte[] bytes = new byte[10240];
                while ((index = inputStream.read(bytes)) != -1) {
                    os.write(bytes, 0, index);
                }
                os.flush();
                os.close();
                inputStream.close();
            }
            catch (Exception e)
            {
                System.err.println("创建文件时出现异常");
                SaveStackTrace.saveStackTrace(e);
                System.exit(-1);
            }
        }
    }

    private void saveLibFiles()
    {
        saveJarFiles("/commons-codec-1.15.jar","/libs/org/apache/commons/codec");
        saveJarFiles("/commons-io-2.11.0.jar","/libs/org/apache/commons/io");
        saveJarFiles("/log4j-api-2.19.0.jar","/libs/org/apache/logging/log4j/");
        saveJarFiles("/log4j-core-2.19.0.jar","/libs/org/apache/logging/log4j/core/");
        //System.exit(0);
    }
    /**
     * 程序的入口点，程序从这里开始运行至结束
     */
    public static void main(String[] args) {
        try {
            logger.info("欢迎使用JavaIM。本项目遵循GPL-3.0协议，请合法使用，作者不承担任何错误使用所造成的后果。项目地址：https://github.com/QiLechan/JavaIM/");
            if (GetAutoSaveDependencyMode()) {
                getInstance().saveLibFiles();
            }
            logger.info("使用客户端模式请输入1，服务端模式请输入2:");
            Scanner sc = new Scanner(System.in);
            int mode = sc.nextInt();
            if (mode == 1) {
                Scanner sc2 = new Scanner(System.in);
                Scanner sc3 = new Scanner(System.in);
                String serverName;
                logger.info("请输入要连接的主机:");
                serverName = sc2.nextLine();
                logger.info("请输入端口:");
                int port = Integer.parseInt(sc3.nextLine());
                new Client(serverName, port);
            } else if (mode == 2) {
                Scanner sc4 = new Scanner(System.in);
                logger.info("请输入监听端口:");
                int port = Integer.parseInt(sc4.nextLine());
                new Server(port);
            } else {
                logger.info("输入值错误，请重新运行程序");
            }
        }
        catch (Exception e)
        {
            SaveStackTrace.saveStackTrace(e);
        }
    }
}
