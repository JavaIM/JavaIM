package org.yuezhikong.utils.checkUpdate.oauth;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class GitHubOAuthAccessTokenAPI {
    private String access_token;
    private String token_type;
    private String scope;
}
