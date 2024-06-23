package org.yuezhikong.utils.checkUpdate.oauth;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class GitHubDeviceCodeAPI {
    private String device_code;
    private String user_code;
    private String verification_uri;
    private int expires_in;
    private int interval;
}
