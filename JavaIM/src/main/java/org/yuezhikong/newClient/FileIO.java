package org.yuezhikong.newClient;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * 跨平台文件io封装
 * Android版依赖于此文件修改io调用方式，请勿删除此文件!
 */
public class FileIO {
    /**
     * 删除文件或文件夹
     * @param file 文件
     * @throws IOException 发生IO错误
     */
    public static void DeleteFile(File file) throws IOException {
        Files.delete(file.toPath());
    }

    /**
     * 创建文件
     * @param file 文件
     * @throws IOException 发生IO错误
     */
    public static void CreateFile(File file) throws IOException {
        Files.createFile(file.toPath());
    }

    /**
     * 创建文件夹
     * @param file 文件
     * @throws IOException 发生IO错误
     */
    public static void CreateDirectory(File file) throws IOException {
        Files.createDirectories(file.toPath());
    }

    /**
     * 从文件读取信息
     * @param file 文件
     * @return 读取到的信息
     * @throws IOException 发生IO错误
     */
    public static String readFileToString(File file) throws IOException {
        return readFileToString(file, StandardCharsets.UTF_8);
    }

    /**
     * 从文件读取信息
     * @param file 文件
     * @param charset 使用的字符集
     * @return 读取到的信息
     * @throws IOException 发生IO错误
     */
    public static String readFileToString(File file, Charset charset) throws IOException {
        return FileUtils.readFileToString(file,charset);
    }

    /**
     * 将数据写入到文件
     * @param file 文件
     * @param data 数据
     * @throws IOException 发生IO错误
     */
    public static void writeStringToFile(File file, String data) throws IOException
    {
        writeStringToFile(file,data,StandardCharsets.UTF_8);
    }

    /**
     * 将数据写入到文件
     * @param file 文件
     * @param data 数据
     * @param charset 使用的字符集
     * @throws IOException 发生IO错误
     */
    public static void writeStringToFile(File file, String data, Charset charset) throws IOException
    {
        FileUtils.writeStringToFile(file, data, charset);
    }
    private FileIO() {}
}
