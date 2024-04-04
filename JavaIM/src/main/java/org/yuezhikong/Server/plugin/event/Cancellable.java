package org.yuezhikong.Server.plugin.event;

/**
 * 可取消(事件)
 * @author AlexLiuDev233
 */
public interface Cancellable {
    /**
     * 是否已经取消
     * @return 是否取消
     */
    boolean isCancelled();

    /**
     * 设置是否取消
     * @param cancelled 是否取消
     */
    void setCancelled(boolean cancelled);
}
