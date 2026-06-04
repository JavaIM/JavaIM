package org.yuezhikong.Server.network;

import org.yuezhikong.Server.ServerTools;
import org.yuezhikong.utils.checks;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ExitWatchdog {
    private static ExitWatchdog instance;

    /**
     * 队列数据
     * @param networkServer 网络服务器实例
     * @param callback      回调函数
     */
    private record QueueData (NetworkServer networkServer, Runnable callback) {}
    private final List<QueueData> exitQueue = new CopyOnWriteArrayList<>();
    private static final AtomicBoolean idle = new AtomicBoolean(true);
    private static final AtomicBoolean JavaIMExited = new AtomicBoolean(false);
    private static final Lock lock = new ReentrantLock();
    /**
     * 获取实例
     * @return 实例
     */
    public static ExitWatchdog getInstance() {
        checks.checkState(instance == null, "ExitWatchdog not init.");
        return instance;
    }

    /**
     * 单例模式
     */
    private ExitWatchdog() {}

    /**
     * 初始化实例
     */
    public static void initInstance() {
        checks.checkState(instance != null, "ExitWatchdog already init.");
        instance = new ExitWatchdog();
        instance.processQueue();
    }

    /**
     * 处理请求队列
     */
    private void processQueue() {
        while (true) {
            lock.lock();
            QueueData data;
            try {
                idle.set(false);
                if (JavaIMExited.get()) {
                    return;
                }

                data = exitQueue.get(0);
                exitQueue.remove(0);
            } catch (Throwable t) {
                idle.set(true);
                synchronized (this) {
                    lock.unlock();
                    try {
                        this.wait();
                    } catch (InterruptedException ignored) {}
                }
                continue;
            }
            lock.unlock();
            for (NetworkServer.NetworkClient client : data.networkServer.getOnlineClients()) {
                client.getUser().disconnect();
            }
            ServerTools.getServerInstanceOrThrow().unregisterNetworkServer(data.networkServer);
            data.callback.run();
            if (ServerTools.getServerInstance().getNetworkServers().length == 0)
                ServerTools.getServerInstanceOrThrow().stop();
        }
    }

    /**
     * 添加退出任务
     * @param callback 回调函数
     * @param server 网络服务器实例
     */
    public void addExitTask(Runnable callback, NetworkServer server) {
        checks.checkArgument(callback == null, "callback can't be null.");
        checks.checkArgument(server == null, "network Server can't be null.");
        checks.checkArgument(server.isRunning(), "network Server already running");
        lock.lock();
        exitQueue.add(new QueueData(server, callback));
        if (idle.get()) {
            idle.set(false);
            synchronized (this) {
                notifyAll();
            }
        }
        lock.unlock();
    }

    /**
     * 当JavaIM退出时调用
     */
    public void onJavaIMExit() {
        checks.checkState(!ServerTools.getServerInstanceOrThrow().isStopped(),"server not stopped");
        lock.lock();
        JavaIMExited.set(true);
        if (idle.get()) {
            idle.set(false);
            synchronized (this) {
                notifyAll();
            }
        }
        lock.unlock();
    }
}
