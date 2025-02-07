package org.yuezhikong.utils;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yuezhikong.SystemConfig;

import java.io.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class MultiThreadDownloadManager implements AutoCloseable {

    private final ExecutorService threadPool = Executors.newFixedThreadPool(SystemConfig.getDownloadParts(), new ThreadFactory() {
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        @Override
        public Thread newThread(@NotNull Runnable r) {
            return new Thread(new ThreadGroup("CheckUpdateThreadPool"),
                    r, "Download File Thread #" + threadNumber.getAndIncrement());
        }
    });

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private volatile boolean canAddNewDownload = true;

    /**
     * 复制流到文件
     *
     * @param ignoredBuilder 用于给子类提供的来源标识
     * @param is             输入流
     * @param dataOutput     数据输出
     * @return 复制是否成功
     * @apiNote fileSize当服务器响应中 Content-Length Header不存在时，输入null
     */
    protected boolean copyStreamToFile(HttpRequest.Builder ignoredBuilder,
                                       InputStream is,
                                       DataOutput dataOutput
    ) throws IOException {
        byte[] buffer = new byte[4096];
        int length;
        while ((length = is.read(buffer)) != -1) {
            dataOutput.write(buffer, 0, length);
        }
        return true;
    }

    /**
     * 当初始化下载时
     *
     * @param requestBuilder http请求
     * @param fileSize       文件长度
     * @param singleThread   是否是单线程
     * @apiNote 用于提供给子类的API
     */
    protected void onDownloadInit(HttpRequest.Builder requestBuilder,
                                  @Nullable Long fileSize,
                                  boolean singleThread) {
    }

    /**
     * 当下载结束时
     *
     * @param requestBuilder http请求
     * @apiNote 用于提供给子类的API
     */
    protected void onDownloadComplete(HttpRequest.Builder requestBuilder) {
    }

    /**
     * 多线程下载文件
     *
     * @param requestBuilder http请求
     * @param file           下载到的文件
     * @return 下载是否完成
     * @throws IllegalStateException 下载器已经被关闭
     * @throws IOException           IO错误
     * @throws InterruptedException  线程被中断
     */
    @Contract(pure = true)
    public boolean downloadFile(HttpRequest.Builder requestBuilder, File file) throws IllegalStateException, IOException, InterruptedException {
        if (canAddNewDownload) {
            synchronized (this) {
                if (canAddNewDownload) {
                    boolean bool;
                    try {
                        bool = beginDownload(requestBuilder, file);
                    } catch (Throwable t) {
                        onDownloadComplete(requestBuilder);
                        throw t;
                    }
                    onDownloadComplete(requestBuilder);
                    return bool;
                }
            }
        }
        throw new IllegalStateException("Cannot add new download, the download manager is closed");
    }

    /**
     * 开始多线程下载文件
     *
     * @param requestBuilder http请求
     * @param file           下载到的文件
     * @return 下载是否成功
     * @throws IOException          IO错误
     * @throws InterruptedException 线程被中断
     */
    @Contract(pure = true)
    private boolean beginDownload(HttpRequest.Builder requestBuilder, File file) throws IOException, InterruptedException {
        HttpResponse<InputStream> response = httpClient.send(requestBuilder.copy().build(), HttpResponse.BodyHandlers.ofInputStream());
        long fileSize = response.headers().firstValue("Content-Length").map(Long::parseLong).orElse(-1L);
        String acceptRangeMode = response.headers().firstValue("Accept-Ranges").orElse("none");

        if (fileSize == -1) {
            log.info("服务器返回的 Content-Length Header不存在，将使用单线程下载");
            onDownloadInit(requestBuilder, null, true);
            return copyStreamToFile(requestBuilder, response.body(), new DataOutputStream(new FileOutputStream(file)));
        }
        if (!acceptRangeMode.equals("bytes")) {
            log.info("服务器返回不允许/不受支持的分片加载类型，将使用单线程下载");
            onDownloadInit(requestBuilder, fileSize, true);
            return copyStreamToFile(requestBuilder, response.body(), new DataOutputStream(new FileOutputStream(file)));
        }

        onDownloadInit(requestBuilder, fileSize, false);
        List<Future<Boolean>> futures = new ArrayList<>();
        // 计算每个部分的大小
        long partSize = fileSize / SystemConfig.getDownloadParts();
        long remainingBytes = fileSize % SystemConfig.getDownloadParts();

        for (int i = 0; i < SystemConfig.getDownloadParts(); i++) {
            // 计算当前部分的起始和结束字节
            long start = i * partSize;
            long end = (i == SystemConfig.getDownloadParts() - 1) ? fileSize - 1 : start + partSize - 1;

            // 如果当前部分不是最后一部分，并且剩余字节大于0，将部分大小增加1，直到剩余字节为0
            if (i < remainingBytes && i < SystemConfig.getDownloadParts() - 1) {
                end++;
                remainingBytes--;
            }

            // 创建下载任务
            long finalEnd = end;
            futures.add(threadPool.submit(() -> {
                HttpResponse<InputStream> responsePart = httpClient.send(
                        requestBuilder.copy()
                                .header("Range", String.format("%s=%d-%d", acceptRangeMode, start, finalEnd))
                                .build(), HttpResponse.BodyHandlers.ofInputStream()
                );
                if (responsePart.statusCode() != 206) {
                    return Boolean.FALSE;
                }

                try (InputStream partIs = responsePart.body();
                     RandomAccessFile raf = new RandomAccessFile(file, "rw")
                ) {
                    raf.seek(start);
                    copyStreamToFile(requestBuilder, partIs, raf);
                }
                return Boolean.TRUE;
            }));
        }

        AtomicBoolean success = new AtomicBoolean(true);
        futures.forEach((future) -> {
            try {
                if (!future.get() && success.get()) {
                    log.error("下载文件失败!");
                    log.error("有一个或多个分片的返回不为206!");
                    success.set(false);
                }
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("Download File Error", e);
            }
        });
        return success.get();
    }

    /**
     * 关闭下载器，但是并不会强行终止下载，但是拒绝所有新的请求加入
     */
    @Override
    @MustBeInvokedByOverriders
    public void close() {
        synchronized (this) {
            canAddNewDownload = false;
        }
        threadPool.shutdown();
    }
}
