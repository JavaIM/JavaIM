package org.yuezhikong;

public class config {
    //最大客户端数量，输入-1代表禁用
    private static final int MAX_CLIENT = -1;
    //调试模式
    public static boolean Debug_Mode = false;
    //是否允许自动释放依赖
    private static final boolean Auto_Save_Dependency = false;
    //测试性RSA加密功能
    private static final boolean Test_RSA_Mode = true;

    //MySQL数据库地址
    private static final String MySQLDataBaseHost = "";
    //MySQL数据库端口
    private static final String MySQLDataBasePort = "";
    //MySQL数据库名称
    private static final String MySQLDataBaseName = "";
    //MySQL数据库用户
    private static final String MySQLDataBaseUser = "";
    //MySQL数据库密码
    private static final String MySQLDataBasePasswd = "";
    public static String GetMySQLDataBaseHost() { return MySQLDataBaseHost; }
    public static String GetMySQLDataBasePort() { return MySQLDataBasePort; }
    public static String GetMySQLDataBaseName() { return MySQLDataBaseName; }
    public static String GetMySQLDataBaseUser() { return MySQLDataBaseUser; }
    public static String GetMySQLDataBasePasswd() { return MySQLDataBasePasswd; }
    public static boolean GetRSA_Mode (){ return Test_RSA_Mode; }
    public static int getMaxClient()
    {
        return MAX_CLIENT;
    }
    public static boolean GetDebugMode() { return Debug_Mode; }
    public static boolean GetAutoSaveDependencyMode() { return Auto_Save_Dependency; }
}
