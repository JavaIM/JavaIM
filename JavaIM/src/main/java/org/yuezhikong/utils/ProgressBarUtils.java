package org.yuezhikong.utils;

import me.tongfei.progressbar.ProgressBar;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ProgressBarUtils {

    private ProgressBarUtils() {}

    /**
     * 复制 InputStream 到 OutputStream,并显示进度条
     * @param taskName              进度条任务名称
     * @param progressInitialMax    进度条最大值
     * @param inputStream           输入流
     * @param outputStream          输出流
     * @throws IOException          复制时出现IO错误
     */
    public static void copyStream(@NotNull @Nls String taskName,
                                  @Range(from = 1, to = Long.MAX_VALUE) long progressInitialMax,
                                  @NotNull InputStream inputStream,
                                  @NotNull OutputStream outputStream) throws IOException {
        try (ProgressBar progressBar = new ProgressBar(taskName,progressInitialMax))  {
            byte[] buffer = new byte[4096];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
                // 更新进度
                progressBar.stepBy(length);
            }
        }
    }
}
