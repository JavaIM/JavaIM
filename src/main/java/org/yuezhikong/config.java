package org.yuezhikong;

public class config {
    private static final int MAX_CLIENT = -1;
    private static final boolean Debug_Mode = false;
    private static final boolean Auto_Save_Dependency = false;
    private static final boolean Test_RSA_Mode = true;
    public static boolean GetRSA_Mode (){ return Test_RSA_Mode; }
    public static int getMaxClient()
    {
        return MAX_CLIENT;
    }
    public static boolean GetDebugMode() { return Debug_Mode; }
    public static boolean GetAutoSaveDependencyMode() { return Auto_Save_Dependency; }
}
