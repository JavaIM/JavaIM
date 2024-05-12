package org.yuezhikong;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.yuezhikong.Server.ServerTools;
import org.yuezhikong.utils.SaveStackTrace;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;

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
            SaveStackTrace.saveStackTrace(e);

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
            FileChannel channel = new FileOutputStream(
                    FileName).getChannel();
            String time = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss").format(StartTimeMills);
            writeStringToFileChannel(channel,"----JavaIM Crash Report----");
            writeStringToFileChannel(channel,"Time: "+time);
            writeStringToFileChannel(channel,"Description: "+type);
            writeStringToFileChannel(channel,"JavaIMVersion: "+CodeDynamicConfig.getVersion());
            writeStringToFileChannel(channel,"Thread: "+t.getName());
            writeStringToFileChannelNoAutoAddLineBreak(channel,"\n");
            writeStringToFileChannel(channel, StackTraceOfThrowable);
            writeStringToFileChannel(channel,"-- System Information --");
            writeStringToFileChannel(channel,"JavaIMVersion: "+CodeDynamicConfig.getVersion());
            writeStringToFileChannel(channel,"Operating System: "+System.getProperty("os.name"));
            writeStringToFileChannel(channel,"Java Version: "+System.getProperty("java.version"));
            writeStringToFileChannel(channel,"Java Virtual Machine Version: "+System.getProperty("java.vm.version"));
            writeStringToFileChannel(channel,"CPU Cores: "+Runtime.getRuntime().availableProcessors());
            channel.force(false);
            channel.close();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                Logger logger = org.yuezhikong.utils.logging.Logger.getLogger(CrashReport.class);
                logger.error("程序已崩溃");
                logger.error("详情请查看错误报告");
                logger.error("位于：{}", FileName);
                logger.error("请自行查看");
                try {
                    ServerTools.getServerInstanceOrThrow().stop();
                } catch (IllegalStateException ignored) {}
            }));
            System.exit(1);
        } catch (IOException ex) {
            SaveStackTrace.saveStackTrace(ex);
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
