package org.yuezhikong.Server.UserData;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class userUploadFile {
    private String userId;
    private String ownFile;
    private String origFileName;
}
