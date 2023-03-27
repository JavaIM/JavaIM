package org.yuezhikong;

public final class CodeDynamicConfig {
    //静态区，不允许修改
    //本程序版本：
    private static final String Version = "v1.1.1-pre2";
    //目前最新的Database协议版本
    private static final int TheLatestDatabaseProtocolVersion = 2;
    //调试模式
    private static final boolean Debug_Mode = false;
    //RSA加密功能
    private static final boolean RSA_Mode = true;
    //插件系统
    private static final boolean PluginSystem = true;
    //本版本为实验性版本
    private static final boolean ThisVersionIsExpVersion = false;
    //如果为测试性版本，则信息为：
    private static final String ExpVersionText = "此版本包括一个测试性AES加密系统," +
            "此系统有助于解决纯RSA加密造成的卡顿问题";
    //AES加密系统（依赖于RSA加密系统）
    private static final boolean AES_Mode = true;

    //动态区，可动态通过配置文件修改
    //最大客户端数量，输入-1代表禁用
    public static int MAX_CLIENT = -1;
    //是否启用登录系统
    public static boolean EnableLoginSystem = true;
    //是否使用SQLITE
    public static boolean Use_SQLITE_Mode = true;
    //MySQL数据库地址
    public static String MySQLDataBaseHost = "127.0.0.1";
    //MySQL数据库端口
    public static String MySQLDataBasePort = "3306";
    //MySQL数据库名称
    public static String MySQLDataBaseName = "JavaIM";
    //MySQL数据库用户
    public static String MySQLDataBaseUser = "JavaIM";
    //MySQL数据库密码
    public static String MySQLDataBasePasswd = "JavaIM";

    public static boolean GetPluginSystemMode() { return PluginSystem; }
    public static boolean GetSQLITEMode() { return Use_SQLITE_Mode; }
    public static int GetDatabaseProtocolVersion() { return TheLatestDatabaseProtocolVersion; }
    public static boolean GetEnableLoginSystem() { return EnableLoginSystem; }
    public static String GetMySQLDataBaseHost() { return MySQLDataBaseHost; }
    public static String GetMySQLDataBasePort() { return MySQLDataBasePort; }
    public static String GetMySQLDataBaseName() { return MySQLDataBaseName; }
    public static String GetMySQLDataBaseUser() { return MySQLDataBaseUser; }
    public static String GetMySQLDataBasePasswd() { return MySQLDataBasePasswd; }
    public static boolean GetRSA_Mode (){ return RSA_Mode; }
    public static int getMaxClient() { return MAX_CLIENT; }
    public static boolean GetDebugMode() { return Debug_Mode; }
    public static boolean isThisVersionIsExpVersion() {
        return ThisVersionIsExpVersion;
    }

    public static String getVersion() {
        return Version;
    }

    public static boolean isAES_Mode() {
        return AES_Mode;
    }

    public static String getExpVersionText() {
        return ExpVersionText;
    }
}
