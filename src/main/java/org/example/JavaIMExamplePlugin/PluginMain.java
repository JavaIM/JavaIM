package org.example.JavaIMExamplePlugin;

import org.yuezhikong.newServer.ServerInterface;
import org.yuezhikong.newServer.plugin.Plugin.Plugin;
import org.yuezhikong.newServer.plugin.Plugin.PluginData;
import org.yuezhikong.newServer.plugin.event.EventHandler;
import org.yuezhikong.newServer.plugin.event.Listener;
import org.yuezhikong.newServer.plugin.event.events.PreLoginEvent;
import org.yuezhikong.newServer.plugin.event.events.UserLoginEvent;

public class PluginMain implements Plugin, Listener {

    @Override
    public void onLoad(PluginData pluginData) {
        ServerInterface.getServer().getLogger().info("[ExamplePlugin] 插件正在被加载");
        pluginData.AddEventListener(this);
        pluginData.AddEventListener(new org.example.JavaIMExamplePlugin.Listener());
    }

    @Override
    public void onUnload() {
        ServerInterface.getServer().getLogger().info("[ExamplePlugin] 插件正在被卸载");
    }

    @EventHandler
    public void onPreLogin(PreLoginEvent event)
    {
        ServerInterface.getServer().getLogger().info("[ExamplePlugin] 用户："+event.getUserName()+"正在登录");
        ServerInterface.getServer().getLogger().info("[ExamplePlugin] 本消息来自PluginMain");
    }

    @EventHandler
    public void onLogin(UserLoginEvent event)
    {
        ServerInterface.getServer().getLogger().info("[ExamplePlugin] 用户："+event.UserName()+"登录成功！");
        ServerInterface.getServer().getLogger().info("[ExamplePlugin] 本消息来自PluginMain");
    }
}
