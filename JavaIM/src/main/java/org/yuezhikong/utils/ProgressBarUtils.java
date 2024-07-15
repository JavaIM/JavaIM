package org.yuezhikong.utils;

import me.tongfei.progressbar.ProgressBar;
import org.jetbrains.annotations.*;

import java.io.*;
import java.net.http.HttpRequest;
import java.util.HashMap;
import java.util.Map;

public class ProgressBarUtils {

    private ProgressBarUtils() {}

    /**
     * 复制 InputStream 到 DataOutput, 并显示进度条
     * @param taskName              进度条任务名称
     * @param progressInitialMax    进度条最大值
     * @param inputStream           输入流
     * @param dataOutputObject      数据输出
     * @throws IOException          复制时出现IO错误
     */
    public static void copyStream(@NotNull @Nls String taskName,
                                  @Range(from = 1, to = Long.MAX_VALUE) long progressInitialMax,
                                  @NotNull InputStream inputStream,
                                  @NotNull DataOutput dataOutputObject) throws IOException {
        try (ProgressBar progressBar = new ProgressBar(taskName,progressInitialMax))  {
            copyStreamOnlyUpdateProgress(progressBar, inputStream, dataOutputObject);
        }
    }

    /**
     * 复制 InputStream 到 DataOutput, 但是只更新进度条
     * @param progressBar             进度条
     * @param inputStream             输入流
     * @param dataOutputObject        数据输出
     * @throws IOException            复制时出现IO错误
     */
    public static void copyStreamOnlyUpdateProgress(ProgressBar progressBar, InputStream inputStream, DataOutput dataOutputObject) throws IOException {
        byte[] buffer = new byte[4096];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            dataOutputObject.write(buffer, 0, length);
            // 更新进度
            progressBar.stepBy(length);
        }
    }

    private static class ProgressBarDownloadManager extends MultiThreadDownloadManager {
        private record request(@Nullable ProgressBar progressBar, boolean singleThread) {}
        private final Map<HttpRequest.Builder, request> requestMap = new HashMap<>();

        private final boolean givenProgressBar;
        private final ProgressBar progressBar;
        private final String taskName;

        public ProgressBarDownloadManager(@NotNull ProgressBar updateProgressBar) {
            givenProgressBar = true;
            progressBar = updateProgressBar;
            taskName = null;
        }

        public ProgressBarDownloadManager(String taskName) {
            givenProgressBar = false;
            progressBar = null;
            this.taskName = taskName;
        }

        @Override
        protected void onDownloadInit(HttpRequest.Builder requestBuilder, Long fileSize, boolean singleThread) {
            if (givenProgressBar)
                return;
            ProgressBar pb;
            if (fileSize != null)
                pb = new ProgressBar(taskName, fileSize);
            else
                pb = null;
            requestMap.put(requestBuilder, new request(pb, singleThread));
        }

        @Override
        protected void onDownloadComplete(HttpRequest.Builder requestBuilder) {
            if (givenProgressBar)
                return;
            request req = requestMap.remove(requestBuilder);
            if (req == null)
                return;
            ProgressBar progressBar = req.progressBar();
            if (progressBar == null)
                return;
            progressBar.close();
        }

        @Override
        protected boolean copyStreamToFile(HttpRequest.Builder requestBuilder,InputStream is, DataOutput dataOutput) throws IOException {
            ProgressBar progressBar;
            if (givenProgressBar) {
                progressBar = this.progressBar;
            } else {
                progressBar = requestMap.get(requestBuilder).progressBar();
            }
            if (progressBar == null)
                return super.copyStreamToFile(requestBuilder, is, dataOutput);
            ProgressBarUtils.copyStreamOnlyUpdateProgress(progressBar, is, dataOutput);
            return true;
        }
    }

    /**
     * 下载文件，创建新进度条
     * @param taskName                  任务名
     * @param requestBuilder            http请求
     * @param file                      下载到的文件
     * @throws IOException              IO错误
     * @throws InterruptedException     线程被中断
     * @return 下载是否完成
     */
    @Contract(pure = true)
    public static boolean downloadFile(@NotNull @Nls String taskName, @NotNull HttpRequest.Builder requestBuilder, @NotNull File file) throws IOException, InterruptedException {
        try (MultiThreadDownloadManager downloadManager = new ProgressBarDownloadManager(taskName)) {
            return downloadManager.downloadFile(requestBuilder, file);
        }
    }

    /**
     * 下载文件，更新旧进度条
     * @param progressBar               进度条
     * @param requestBuilder            http请求
     * @param file                      下载到的文件
     * @throws IOException              IO错误
     * @throws InterruptedException     线程被中断
     * @return 下载是否完成
     * @apiNote 请注意进度条长度!
     */
    @SuppressWarnings("unused")
    @Contract(pure = true)
    public static boolean downloadFileOnlyUpdateProgress(@NotNull ProgressBar progressBar, @NotNull HttpRequest.Builder requestBuilder, @NotNull File file) throws IOException, InterruptedException {
        try (MultiThreadDownloadManager downloadManager = new ProgressBarDownloadManager(progressBar)) {
            return downloadManager.downloadFile(requestBuilder, file);
        }
    }
}
