package org.yuezhikong;

public final class CodeDynamicConfig {
    //静态区，不允许修改
    //目前最新的Database协议版本
    private static final int TheLatestDatabaseProtocolVersion = 2;
    //调试模式
    private static final boolean Debug_Mode = false;
    //是否允许自动释放依赖
    private static final boolean Auto_Save_Dependency = false;
    //RSA加密功能
    private static final boolean RSA_Mode = true;

    //动态区，可动态通过配置文件修改
    //最大客户端数量，输入-1代表禁用
    public static int MAX_CLIENT;
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
    public static boolean GetAutoSaveDependencyMode() { return Auto_Save_Dependency; }
}
