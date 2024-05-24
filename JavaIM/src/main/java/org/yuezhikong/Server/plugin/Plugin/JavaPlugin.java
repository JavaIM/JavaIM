package org.yuezhikong.Server.plugin.Plugin;

@SuppressWarnings("unused")
public abstract class JavaPlugin implements Plugin{
    private PluginData pluginData;
    @Override
    public void setPluginData(PluginData pluginData) {
        this.pluginData = pluginData;
    }

    @Override
    public PluginData getPluginData() {
        return pluginData;
    }

    @Override
    public void onPreload() {
        // 默认在Preload阶段不执行任何操作
    }
}
