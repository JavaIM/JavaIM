package org.example.JavaIMExamplePlugin;

import org.yuezhikong.newServer.ServerInterface;
import org.yuezhikong.newServer.plugin.event.EventHandler;
import org.yuezhikong.newServer.plugin.event.events.PreLoginEvent;
import org.yuezhikong.newServer.plugin.event.events.UserLoginEvent;

public class Listener implements org.yuezhikong.newServer.plugin.event.Listener {

    @EventHandler
    public void onPreLogin(PreLoginEvent event)
    {
        ServerInterface.getServer().getLogger().info("[ExamplePlugin] 用户："+event.getUserName()+"正在登录");
        ServerInterface.getServer().getLogger().info("[ExamplePlugin] 本消息来自Listener class");
    }

    @EventHandler
    public void onLogin(UserLoginEvent event)
    {
        ServerInterface.getServer().getLogger().info("[ExamplePlugin] 用户："+event.UserName()+"登录成功！");
        ServerInterface.getServer().getLogger().info("[ExamplePlugin] 本消息来自Listener class");
    }
}
