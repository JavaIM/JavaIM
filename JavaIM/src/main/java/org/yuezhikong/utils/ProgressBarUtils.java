package org.yuezhikong.utils;

import me.tongfei.progressbar.ProgressBar;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.io.*;

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
            copyStreamAndUpdateProgressBar(progressBar, inputStream, dataOutputObject);
        }
    }

    /**
     * 复制 InputStream 到 DataOutput, 但是只更新进度条
     * @param progressBar             进度条
     * @param inputStream             输入流
     * @param dataOutputObject        数据输出
     * @throws IOException            复制时出现IO错误
     */
    public static void copyStreamAndUpdateProgressBar(ProgressBar progressBar, InputStream inputStream, DataOutput dataOutputObject) throws IOException {
        byte[] buffer = new byte[4096];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            dataOutputObject.write(buffer, 0, length);
            // 更新进度
            progressBar.stepBy(length);
        }
    }
}
