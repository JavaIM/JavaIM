package org.yuezhikong.utils.checkUpdate.oauth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GitHubOAuthErrorAPI {
    private String error;
    private String error_description;
    private String error_uri;
}
