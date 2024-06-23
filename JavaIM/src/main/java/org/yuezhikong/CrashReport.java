package org.yuezhikong;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yuezhikong.Server.ServerTools;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;

@Slf4j
public class CrashReport implements Thread.UncaughtExceptionHandler {

    private static volatile CrashReport Instance;
    /**
     * 单例，使用getCrashReport()方法获取实例
     */
    private CrashReport()
    {

    }

    public static CrashReport getCrashReport()
    {
        if (Instance == null)
        {
            synchronized (CrashReport.class)
            {
                if (Instance == null)
                {
                    Instance = new CrashReport();
                }
            }
        }
        return Instance;
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        JavaIMCrashReport("uncaughtException",e,t);
    }

    protected static void JavaIMCrashReport(String type,Throwable e,Thread t)
    {
        long StartTimeMills = System.currentTimeMillis();
        String StackTraceOfThrowable;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
        try {
            StringWriter writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter(writer);
            e.printStackTrace(printWriter);
            StackTraceOfThrowable = writer.toString();
            printWriter.close();
            writer.close();
            String FileName = "./crash-reports/crash-report-"+formatter.format(StartTimeMills)+".txt";

            try {
                Files.createDirectory(Paths.get("./crash-reports"));
            } catch (FileAlreadyExistsException ignored) {}
            Files.createFile(Paths.get(FileName));
            try (FileOutputStream stream = new FileOutputStream(FileName)) {
                FileChannel channel = stream.getChannel();
                String time = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss").format(StartTimeMills);
                writeStringToFileChannel(channel, "----JavaIM Crash Report----");
                writeStringToFileChannel(channel, "Time: " + time);
                writeStringToFileChannel(channel, "Description: " + type);
                writeStringToFileChannel(channel, "JavaIMVersion: " + CodeDynamicConfig.getVersion());
                writeStringToFileChannel(channel, "Thread: " + t.getName());
                writeStringToFileChannelNoAutoAddLineBreak(channel, "\n");
                writeStringToFileChannel(channel, StackTraceOfThrowable);
                writeStringToFileChannel(channel, "-- System Information --");
                writeStringToFileChannel(channel, "JavaIMVersion: " + CodeDynamicConfig.getVersion());
                writeStringToFileChannel(channel, "Operating System: " + System.getProperty("os.name"));
                writeStringToFileChannel(channel, "Java Version: " + System.getProperty("java.version"));
                writeStringToFileChannel(channel, "Java Virtual Machine Version: " + System.getProperty("java.vm.version"));
                writeStringToFileChannel(channel, "CPU Cores: " + Runtime.getRuntime().availableProcessors());
                channel.force(false);
                channel.close();
            }
            log.error("出现错误!",e);

            log.error("程序已崩溃");
            log.error("详情请查看错误报告");
            log.error("位于：{}", FileName);
            log.error("请自行查看");
            System.exit(1);
        } catch (IOException ex) {
            log.error("出现错误!",ex);
        }
    }
    private static void writeStringToFileChannelNoAutoAddLineBreak(FileChannel channel, String writeData) throws IOException{
        ByteBuffer buf = ByteBuffer.allocate(5);
        byte[] data = writeData.getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < data.length; ) {
            buf.put(data, i, Math.min(data.length - i, buf.limit() - buf.position()));
            buf.flip();
            i += channel.write(buf);
            buf.compact();
        }
    }

    public static void writeStringToFileChannel(@NotNull FileChannel channel, @NotNull String writeData) throws IOException {
        writeStringToFileChannelNoAutoAddLineBreak(channel,writeData+"\n");
    }

    public static void failedException(Throwable e) {
        JavaIMCrashReport("Failed by Exception",e,Thread.currentThread());
    }
}
