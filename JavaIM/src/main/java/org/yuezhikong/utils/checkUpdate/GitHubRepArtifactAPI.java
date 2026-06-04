package org.yuezhikong.utils.checkUpdate;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class GitHubRepArtifactAPI {
    private int total_count;
    private List<ArtifactsBean> artifacts;

    @Setter
    @Getter
    public static class ArtifactsBean {
        private int id;
        private String node_id;
        private String name;
        private int size_in_bytes;
        private String url;
        private String archive_download_url;
        private boolean expired;
        private String created_at;
        private String updated_at;
        private String expires_at;
        private WorkflowRunBean workflow_run;

        @Setter
        @Getter
        public static class WorkflowRunBean {
            private long id;
            private int repository_id;
            private int head_repository_id;
            private String head_branch;
            private String head_sha;
        }
    }
}
