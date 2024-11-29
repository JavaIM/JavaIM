package org.yuezhikong;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;

@Slf4j
public class CrashReport implements Thread.UncaughtExceptionHandler {

    private static volatile CrashReport Instance;

    /**
     * 单例，使用getCrashReport()方法获取实例
     */
    private CrashReport() {

    }

    public static CrashReport getCrashReport() {
        if (Instance == null) {
            synchronized (CrashReport.class) {
                if (Instance == null) {
                    Instance = new CrashReport();
                }
            }
        }
        return Instance;
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        crashReport("出现未捕捉的异常", e, t);
    }

    /**
     * 报告JavaIM崩溃信息
     *
     * @param type 类型
     * @param e    异常/JVM错误
     * @param t    触发错误的线程
     */
    public static void crashReport(String type, Throwable e, Thread t) {
        long currentTime = System.currentTimeMillis();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
        try {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String stackTrace = sw.toString();
            String fileName = "crash-report-" + formatter.format(currentTime) + ".txt";

            File crashReportDirectory = new File("./crash-reports");
            if (!crashReportDirectory.isDirectory() && !crashReportDirectory.delete())
                log.info("删除crash-reports文件失败");
            if (!crashReportDirectory.exists() && !crashReportDirectory.mkdirs())
                log.info("创建crash-reports文件夹失败");

            File crashReport = new File(crashReportDirectory, fileName);
            if (crashReport.exists() && !crashReport.delete())
                log.info("删除崩溃报告文件失败");
            if (!crashReport.createNewFile())
                log.info("创建崩溃报告文件失败");

            String time = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss").format(currentTime);
            FileUtils.writeStringToFile(crashReport, "---- JavaIM 崩溃报告 ----" + "\n", StandardCharsets.UTF_8);
            FileUtils.writeStringToFile(crashReport, "时间: " + time + "\n", StandardCharsets.UTF_8, true);
            FileUtils.writeStringToFile(crashReport, "类型: " + type + "\n", StandardCharsets.UTF_8, true);
            Throwable cause = e;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            FileUtils.writeStringToFile(crashReport, "可能导致崩溃的原因: " + cause + "\n", StandardCharsets.UTF_8, true);
            boolean pluginCause;
            if (!stackTrace.contains("org.yuezhikong"))
                pluginCause = true;
            else
                pluginCause = stackTrace.contains("org.yuezhikong.Server.plugin");
            if (pluginCause)
                FileUtils.writeStringToFile(crashReport, "可能导致崩溃的模块: 服务端插件" + "\n", StandardCharsets.UTF_8, true);
            else
                FileUtils.writeStringToFile(crashReport, "可能导致崩溃的模块: JavaIM" + "\n", StandardCharsets.UTF_8, true);
            FileUtils.writeStringToFile(crashReport, "JavaIM版本: " + SystemConfig.getVersion() + "\n", StandardCharsets.UTF_8, true);
            FileUtils.writeStringToFile(crashReport, "出错的线程: " + t.getName() + "\n", StandardCharsets.UTF_8, true);
            FileUtils.writeStringToFile(crashReport, "\n", StandardCharsets.UTF_8, true);
            FileUtils.writeStringToFile(crashReport, stackTrace + "\n", StandardCharsets.UTF_8, true);
            FileUtils.writeStringToFile(crashReport, "-- 系统信息 --" + "\n", StandardCharsets.UTF_8, true);
            FileUtils.writeStringToFile(crashReport, "运行 JavaIM 的操作系统: " + System.getProperty("os.name") + "\n", StandardCharsets.UTF_8, true);
            FileUtils.writeStringToFile(crashReport, "Java版本: " + System.getProperty("java.version") + "\n", StandardCharsets.UTF_8, true);
            FileUtils.writeStringToFile(crashReport, "Java虚拟机版本: " + System.getProperty("java.vm.version") + "\n", StandardCharsets.UTF_8, true);
            FileUtils.writeStringToFile(crashReport, "CPU逻辑处理器数量: " + Runtime.getRuntime().availableProcessors() + "\n", StandardCharsets.UTF_8, true);
            log.error("出现错误!", e);

            log.error("程序已崩溃");
            log.error("详情请查看错误报告");
            log.error("位于：{}", crashReport.getAbsolutePath());
            log.error("请自行查看");
            System.exit(1);
        } catch (IOException ex) {
            log.error("出现错误!", ex);
        }
    }
}
