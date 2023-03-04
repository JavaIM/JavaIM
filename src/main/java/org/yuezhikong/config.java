package org.yuezhikong;

public final class config {
    //最大客户端数量，输入-1代表禁用
    private static final int MAX_CLIENT = -1;
    //目前最新的Database协议版本
    private static final int TheLatestDatabaseProtocolVersion = 2;
    //是否启用登录系统
    private static final boolean EnableLoginSystem = true;
    //调试模式
    public static boolean Debug_Mode = false;
    //是否使用SQLITE
    private static final boolean Use_SQLITE_Mode = true;
    //是否允许自动释放依赖
    private static final boolean Auto_Save_Dependency = false;
    //测试性RSA加密功能
    private static final boolean Test_RSA_Mode = true;

    //MySQL数据库地址
    private static final String MySQLDataBaseHost = "127.0.0.1";
    //MySQL数据库端口
    private static final String MySQLDataBasePort = "3306";
    //MySQL数据库名称
    private static final String MySQLDataBaseName = "JavaIM";
    //MySQL数据库用户
    private static final String MySQLDataBaseUser = "JavaIM";
    //MySQL数据库密码
    private static final String MySQLDataBasePasswd = "JavaIM";

    public static boolean GetSQLITEMode() { return Use_SQLITE_Mode; }
    public static int GetDatabaseProtocolVersion() { return TheLatestDatabaseProtocolVersion; }
    public static boolean GetEnableLoginSystem() { return EnableLoginSystem; }
    public static String GetMySQLDataBaseHost() { return MySQLDataBaseHost; }
    public static String GetMySQLDataBasePort() { return MySQLDataBasePort; }
    public static String GetMySQLDataBaseName() { return MySQLDataBaseName; }
    public static String GetMySQLDataBaseUser() { return MySQLDataBaseUser; }
    public static String GetMySQLDataBasePasswd() { return MySQLDataBasePasswd; }
    public static boolean GetRSA_Mode (){ return Test_RSA_Mode; }
    public static int getMaxClient() { return MAX_CLIENT; }
    public static boolean GetDebugMode() { return Debug_Mode; }
    public static boolean GetAutoSaveDependencyMode() { return Auto_Save_Dependency; }
}
