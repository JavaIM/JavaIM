package org.yuezhikong.utils.checkUpdate;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.yuezhikong.CodeDynamicConfig;
import org.yuezhikong.utils.ProgressBarUtils;
import org.yuezhikong.utils.checkUpdate.oauth.GitHubDeviceCodeAPI;
import org.yuezhikong.utils.checkUpdate.oauth.GitHubOAuthAccessTokenAPI;

import javax.net.ssl.SSLHandshakeException;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Slf4j
public class CheckUpdate {
    private final static Gson gson = new Gson();
    /**
     * 检查更新
     * @param installUpdate 是否允许自动安装(覆盖源文件+关闭JVM)
     * @param githubAccessToken accessToken
     */
    public static void checkUpdate(boolean installUpdate, String githubAccessToken) {
        long currentUnixTime = System.currentTimeMillis();
        log.info("正在检查更新(这可能需要一段时间...)");
        String commits,currentFileGitCommitId;
        // 获取保存的本文件commit Id
        try {
            Properties properties = new Properties();
            InputStream is = CheckUpdate.class.getClassLoader().getResourceAsStream("git.properties");
            if (is == null) {
                log.error("检查更新失败，无法读取git.properties文件!");
                log.error("取消检查更新!");
                return;
            }
            properties.load(is);
            currentFileGitCommitId = properties.getProperty("git.commit.id.full");
        } catch (IOException e) {
            log.error("检查更新失败，无法读取git.properties文件!");
            log.error("取消检查更新!");
            return;
        }
        HttpClient httpClient = HttpClient.newHttpClient();
        // 下载远程 git log
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.github.com/repos/JavaIM/JavaIM/commits"))
                    .timeout(Duration.ofSeconds(30))
                    .header("User-Agent", "JavaIM updateHelper")
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            commits = response.body();
        } catch (IOException e) {
            if (e instanceof SSLHandshakeException) {
                log.error("SSL 握手失败，请检查网络!");
                log.error("远程证书异常!");
            }
            log.error("检查更新失败，网络错误!");
            log.error("取消检查更新!");
            return;
        } catch (InterruptedException e) {
            log.error("主线程收到中断，程序结束...");
            System.exit(0);
            return;
        }

        // 解析
        JsonArray jsonArray = JsonParser.parseString(commits).getAsJsonArray();
        if (jsonArray.isEmpty()) {
            log.error("GitHub 返回的commit数据为空!");
            log.error("取消检查更新!");
            return;
        }
        GitHubRepoCommitAPI commitAPI = gson.fromJson(jsonArray.get(0), GitHubRepoCommitAPI.class);
        log.info("更新检查完成，耗时: {}ms", System.currentTimeMillis() - currentUnixTime);
        if (commitAPI.getSha().equals(currentFileGitCommitId))
            return;// 说明已是最新
        log.info("发现新版本 JavaIM!");
        log.info("commit id: {}", commitAPI.getSha());
        log.info("commit message: {}", commitAPI.getCommit().getMessage());
        log.info("author: {}", commitAPI.getCommit().getAuthor().getName());
        log.info("committer: {}", commitAPI.getCommit().getCommitter().getName());
        if (!installUpdate)
            return;// 判断是否自动安装更新
        installUpdate(githubAccessToken, commitAPI.getSha(),httpClient);
    }

    /**
     * 安装更新
     * @param githubAccessToken GitHub access token
     * @param commitSha commit的sha
     * @param httpClient http客户端
     */
    private static void installUpdate(String githubAccessToken, String commitSha, HttpClient httpClient) {
        log.info("因为您允许自动安装更新，所以正在下载更新并安装...");
        String artifact;

        // 下载action CI列表
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.github.com/repos/JavaIM/JavaIM/actions/artifacts"))
                    .timeout(Duration.ofSeconds(30))
                    .header("Accept","application/json")
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "JavaIM updateHelper")
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            artifact = response.body();
        } catch (IOException e) {
            log.error("检查更新失败，网络错误!");
            log.error("取消检查更新!");
            return;
        } catch (InterruptedException e) {
            log.error("主线程收到中断，程序结束...");
            System.exit(0);
            return;
        }

        GitHubRepArtifactAPI artifactAPI = gson.fromJson(artifact, GitHubRepArtifactAPI.class);
        String actionDownloadUrl = null;
        // 解析，寻找远程最新commit对应的action CI
        for (GitHubRepArtifactAPI.ArtifactsBean artifactBean : artifactAPI.getArtifacts()) {
            if (!artifactBean.getWorkflow_run().getHead_sha().equals(commitSha))
                continue;
            if (artifactBean.isExpired()) {
                log.error("Action Artifact 已过期");
                log.error("取消下载更新!");
                return;
            }
            actionDownloadUrl = artifactBean.getArchive_download_url();
            break;
        }
        if (actionDownloadUrl == null) {
            log.error("未找到 Action Artifact");
            log.error("可能是因为此更新刚刚发布，自动构建还未完成");
            log.error("取消下载更新!");
            return;
        }

        // 获取jar位置
        File jarPath = new File(CheckUpdate.class.getProtectionDomain().getCodeSource().getLocation().getFile());
        if (!githubAccessToken.isEmpty()) {
            // 如果用户有提供github访问令牌，则直接使用该令牌下载更新
            downloadUpdate(jarPath, githubAccessToken, actionDownloadUrl, httpClient);
            return;
        }
        log.info("在install update时要求GitHub授权不是必须的，您可以通过--githubAccessToken选项设置一个有效的访问令牌来跳过");

        // 开始请求 Device Connect
        GitHubDeviceCodeAPI deviceCodeAPIResult;
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://github.com/login/device/code"))
                    .POST(HttpRequest.BodyPublishers.ofString("{\"client_id\":\"Iv23liaNX7FdDSiQmD4Q\"}", StandardCharsets.UTF_8))
                    .header("Accept","application/json")
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "JavaIM updateHelper")
                    .timeout(Duration.ofSeconds(30))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            deviceCodeAPIResult = gson.fromJson(response.body(), GitHubDeviceCodeAPI.class);
        } catch (IOException e) {
            log.error("检查更新失败，网络错误!");
            log.error("取消检查更新!");
            return;
        } catch (InterruptedException e) {
            log.error("主线程收到中断，程序结束...");
            System.exit(0);
            return;
        }

        log.info("请在打开的网页中填写 User Code: {}", deviceCodeAPIResult.getUser_code());

        // 写入剪贴板
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(new StringSelection(deviceCodeAPIResult.getUser_code()), null);
            log.info("已经将 User Code: {} 写入剪贴板",deviceCodeAPIResult.getUser_code());
        } catch (Throwable throwable) {
            log.error("剪贴板写入失败");
        }
        // 调用浏览器启动
        try {
            Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
            if (desktop == null || !desktop.isSupported(Desktop.Action.BROWSE)) {
                log.error("没有找到默认浏览器!");
                log.error("请您手动访问 {} 来进行 Device Connect", deviceCodeAPIResult.getVerification_uri());
            } else {
                try {
                    desktop.browse(URI.create(deviceCodeAPIResult.getVerification_uri()));
                } catch (IOException e) {
                    log.error("默认浏览器启动失败!");
                    log.error("请您手动访问 {} 来进行 Device Connect", deviceCodeAPIResult.getVerification_uri());
                }
            }
        } catch (Throwable throwable) {
            log.error("浏览器调用失败");
        }

        // 等待用户授权
        String accessToken;
        HttpRequest accessTokenRequest = HttpRequest.newBuilder(URI.create("https://github.com/login/oauth/access_token"))
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"client_id\":\"Iv23liaNX7FdDSiQmD4Q\"," +
                                "\"device_code\":\""+deviceCodeAPIResult.getDevice_code()+"\"," +
                                "\"grant_type\":\"urn:ietf:params:oauth:grant-type:device_code\"",StandardCharsets.UTF_8)
                )
                .header("Accept","application/json")
                .header("Content-Type", "application/json")
                .header("User-Agent", "JavaIM updateHelper")
                .timeout(Duration.ofSeconds(30))
                .build();
        while (true) {
            try {
                String body = httpClient.send(accessTokenRequest, HttpResponse.BodyHandlers.ofString()).body();
                if (body.contains("error")) {
                    if (!body.contains("authorization_pending")) {
                        log.error("Github返回的错误代码不是正在等待请求! 原始返回:{}",body);
                        return;
                    }

                    TimeUnit.SECONDS.sleep(deviceCodeAPIResult.getInterval() + 1);
                    continue;
                }
                accessToken = gson.fromJson(body, GitHubOAuthAccessTokenAPI.class).getAccess_token();
                break;
            } catch (IOException e) {
                log.error("检查更新失败，网络错误!");
                log.error("取消检查更新!");
                return;
            } catch (InterruptedException e) {
                log.error("主线程收到中断，程序结束...");
                System.exit(0);
                return;
            }
        }

        log.info("GitHub AccessToken: {}", accessToken);

        // 下载更新
        downloadUpdate(jarPath, accessToken, actionDownloadUrl, httpClient);
    }

    /**
     * 下载更新
     * @param downloadTo jar下载到
     * @param githubAccessToken GitHub access token
     * @param downloadUrl 下载链接
     * @param httpClient http客户端
     */
    private static void downloadUpdate(File downloadTo, String githubAccessToken, String downloadUrl,HttpClient httpClient) {
        // 判断 downloadTo是否可以写入
        if (downloadTo.isDirectory()) {
            log.error("codeSource为文件夹，您当前正在直接执行classes?");
            log.error("无法应用更新!");
            return;
        }
        if (!downloadTo.canWrite()) {
            log.error("无权写入文件!");
            log.error("无法应用更新!");
            return;
        }
        try {
            // 请求 GitHub API,获得重定向位置
            HttpResponse<Void> RedirectRequestResponse = httpClient.send(HttpRequest.newBuilder(URI.create(downloadUrl))
                    .header("Accept","application/vnd.github+json")
                    .header("User-Agent", "JavaIM updateHelper")
                    .header("Authorization","Bearer "+githubAccessToken)
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .timeout(Duration.ofSeconds(30))
                    .build(), HttpResponse.BodyHandlers.discarding());

            Optional<String> redirectLocation = RedirectRequestResponse.headers().firstValue("location");
            if (RedirectRequestResponse.statusCode() != 302 || redirectLocation.isEmpty()) {
                log.error("请求：{} URI，尝试获取重定向位置失败，返回值为:{}",RedirectRequestResponse.uri(), RedirectRequestResponse.statusCode());
                return;
            }

            // 请求 302 得到的位置，下载文件
            log.info("正在下载新版本文件...");
            HttpResponse<InputStream> response = httpClient.send(HttpRequest.newBuilder(URI.create(redirectLocation.get()))
                    .header("Accept","*/*")
                    .header("User-Agent","JavaIM updateHelper")
                    .timeout(Duration.ofMinutes(10))
                    .build(), HttpResponse.BodyHandlers.ofInputStream()
            );
            File tmpZipFile = File.createTempFile("JavaIMUpdate", ".zip");
            tmpZipFile.deleteOnExit();
            long fileSize = response.headers().firstValue("Content-Length").map(Long::parseLong).orElse(-1L);
            String acceptRangeMode = response.headers().firstValue("Accept-Ranges").orElse("none");
            if (acceptRangeMode.equals("none"))
                // 如果 GitHub 禁止了分片下载，恢复传统单线程下载(应该不会发生,仅作为fallback)
                try (InputStream is = response.body(); DataOutputStream os = new DataOutputStream(new FileOutputStream(tmpZipFile))) {
                    if (fileSize == -1) {
                        log.error("远程服务器未返回 Content-Length，无法判断文件大小，无法提供进度条!");
                        IOUtils.copy(is,os);
                    } else
                        ProgressBarUtils.copyStream("下载 JavaIM 更新包", fileSize, is, os);
                }
            // 支持的情况下，使用多线程下载
            else try (ProgressBar progressBar = new ProgressBar("下载 JavaIM 更新包", fileSize))  {
                if (!acceptRangeMode.equals("bytes"))
                    log.warn("GitHub 返回支持分片下载，但类型并非bytes，而是{}，在这种情况下，多线程下载可能不可靠!", acceptRangeMode);
                ExecutorService threadPool = Executors.newFixedThreadPool(CodeDynamicConfig.getDownloadParts(), new ThreadFactory() {
                    private final AtomicInteger threadNumber = new AtomicInteger(1);
                    @Override
                    public Thread newThread(@NotNull Runnable r) {
                        return new Thread(new ThreadGroup("CheckUpdateThreadPool"),
                                r,"Download File Thread #"+threadNumber.getAndIncrement());
                    }
                });

                List<Future<Boolean>> futures = new ArrayList<>();
                // 计算每个部分的大小
                long partSize = fileSize / CodeDynamicConfig.getDownloadParts();
                long remainingBytes = fileSize % CodeDynamicConfig.getDownloadParts();

                for (int i = 0; i < CodeDynamicConfig.getDownloadParts(); i++) {
                    // 计算当前部分的起始和结束字节
                    long start = i * partSize;
                    long end = (i == CodeDynamicConfig.getDownloadParts() - 1) ? fileSize - 1 : start + partSize - 1;

                    // 如果当前部分不是最后一部分，并且剩余字节大于0，将部分大小增加1，直到剩余字节为0
                    if (i < remainingBytes && i < CodeDynamicConfig.getDownloadParts() - 1) {
                        end++;
                        remainingBytes--;
                    }

                    // 创建下载任务
                    long finalEnd = end;
                    futures.add(threadPool.submit(() -> {
                        HttpResponse<InputStream> responsePart = httpClient.send(HttpRequest.newBuilder(URI.create(redirectLocation.get()))
                                .header("Accept","*/*")
                                .header("User-Agent","JavaIM updateHelper")
                                .header("Range",String.format("%s=%d-%d",acceptRangeMode, start, finalEnd))
                                .timeout(Duration.ofMinutes(10))
                                .build(), HttpResponse.BodyHandlers.ofInputStream()
                        );
                        if (responsePart.statusCode() != 206) {
                            return Boolean.FALSE;
                        }

                        try (InputStream partIs = responsePart.body();
                             RandomAccessFile raf = new RandomAccessFile(tmpZipFile, "rw")
                        ) {
                            raf.seek(start);
                            ProgressBarUtils.copyStreamAndUpdateProgressBar(progressBar, partIs, raf);
                        }
                        return Boolean.TRUE;
                    }));
                }

                AtomicBoolean stop = new AtomicBoolean(false);
                futures.forEach((future) -> {
                    try {
                        if (!future.get() && !stop.get()) {
                            log.error("下载文件失败!");
                            log.error("有一个或多个分片的返回不为206!");
                            stop.set(true);
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException("Download File Error",e);
                    }
                });
                if (stop.get())
                    return;
            }
            log.info("正在解压缩...");
            try (ZipFile zipFile = new ZipFile(tmpZipFile)) {
                Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
                while (enumeration.hasMoreElements()) {
                    ZipEntry zipEntry = enumeration.nextElement();
                    if (!zipEntry.getName().contains("SNAPSHOT"))
                        continue;
                    try (InputStream is = zipFile.getInputStream(zipEntry); DataOutputStream os = new DataOutputStream(new FileOutputStream(downloadTo)))  {
                        if (zipEntry.getSize() == -1) {
                            log.error("我们不知道文件大小，无法提供进度条!");
                            IOUtils.copy(is,os);
                        } else
                            ProgressBarUtils.copyStream("解压 JavaIM 更新包", zipEntry.getSize(), is, os);
                    }

                    log.info("更新完成!");
                    log.info("即将关闭JVM...");
                    System.exit(0);
                }
            }
        } catch (IOException e) {
            log.error("检查更新失败，网络错误!");
            log.error("取消检查更新!");
        } catch (InterruptedException e) {
            log.error("主线程收到中断，程序结束...");
            System.exit(0);
        }
    }
}
