package org.yuezhikong.Server.userData;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class userInformation {
    private int Permission;
    private String UserName;
    private String Passwd;
    private String salt;
    private String token;
    private String userId;
    private String avatar;
}
