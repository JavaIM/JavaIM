package org.yuezhikong.Server.network;

import org.yuezhikong.Server.UserData.tcpUser.tcpUser;

import java.net.SocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

/**
 * 定义了一个标准JavaIM网络服务器接口
 * 所有网络服务器为了兼容性，必须实现此接口
 */
public interface NetworkServer {
    interface NetworkClient {
        /**
         * 获取客户端的IP地址
         * @return IP地址
         */
        SocketAddress getSocketAddress();

        /**
         * 发信给此客户端
         * @throws IllegalStateException 客户端已断开连接
         */
        void send(String message) throws IllegalStateException;

        /**
         * 是否在线
         * @return 是否在线
         */
        boolean isOnline();

        /**
         * 断开连接
         */
        void disconnect();

        /**
         * 获取user
         */
        tcpUser getUser();
    }

    /**
     * 启动服务器
     * @param ListenPort 监听端口
     * @param forkJoinPool 用于启动网络层的ForkJoinPool
     * @throws IllegalStateException 服务器已经启动
     * @apiNote 堵塞函数
     */
    void start(int ListenPort, ForkJoinPool forkJoinPool) throws IllegalStateException;

    /**
     * 获取在线客户端列表
     */
    NetworkClient[] getOnlineClients();

    /**
     * 是否正在运行
     */
    boolean isRunning();

    /**
     * 关闭服务器
     * @throws IllegalStateException 服务器未启动
     */
    void stop() throws IllegalStateException;

    /**
     * 获取网络层IO线程池
     * @return IO线程池
     */
    ExecutorService getIOThreadPool();
}
