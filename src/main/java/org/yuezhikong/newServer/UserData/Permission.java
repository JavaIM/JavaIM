package org.yuezhikong.newServer.UserData;

public enum Permission {
    ADMIN,
    NORMAL,
    BAN;
    public static Permission ToPermission(int PermissionLevel) {
        if (PermissionLevel == 1)
            return Permission.ADMIN;
        if (PermissionLevel == 0)
            return Permission.NORMAL;
        if (PermissionLevel == -1)
            return Permission.BAN;
        return Permission.NORMAL;
    }
}
