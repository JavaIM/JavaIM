package org.yuezhikong.utils.checkUpdate;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class GitHubRepoCommitAPI {
    private String sha;
    private String node_id;
    private CommitBean commit;
    private String url;
    private String html_url;
    private String comments_url;
    private AuthorBeanX author;
    private CommitterBeanX committer;
    private List<ParentsBean> parents;

    @Setter
    @Getter
    public static class CommitBean {
        private AuthorBean author;
        private CommitterBean committer;
        private String message;
        private TreeBean tree;
        private String url;
        private int comment_count;
        private VerificationBean verification;

        @Setter
        @Getter
        public static class AuthorBean {
            private String name;
            private String email;
            private String date;
        }

        @Setter
        @Getter
        public static class CommitterBean {
            private String name;
            private String email;
            private String date;
        }

        @Setter
        @Getter
        public static class TreeBean {
            private String sha;
            private String url;
        }

        @Setter
        @Getter
        public static class VerificationBean {
            private boolean verified;
            private String reason;
            private String signature;
            private String payload;
        }
    }

    @Setter
    @Getter
    public static class AuthorBeanX {
        private String login;
        private int id;
        private String node_id;
        private String avatar_url;
        private String gravatar_id;
        private String url;
        private String html_url;
        private String followers_url;
        private String following_url;
        private String gists_url;
        private String starred_url;
        private String subscriptions_url;
        private String organizations_url;
        private String repos_url;
        private String events_url;
        private String received_events_url;
        private String type;
        private boolean site_admin;
    }

    @Setter
    @Getter
    public static class CommitterBeanX {
        private String login;
        private int id;
        private String node_id;
        private String avatar_url;
        private String gravatar_id;
        private String url;
        private String html_url;
        private String followers_url;
        private String following_url;
        private String gists_url;
        private String starred_url;
        private String subscriptions_url;
        private String organizations_url;
        private String repos_url;
        private String events_url;
        private String received_events_url;
        private String type;
        private boolean site_admin;
    }

    @Setter
    @Getter
    public static class ParentsBean {
        private String sha;
        private String url;
        private String html_url;
    }
}
