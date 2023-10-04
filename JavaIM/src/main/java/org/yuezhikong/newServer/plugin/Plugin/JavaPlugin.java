package org.yuezhikong.newServer.plugin.Plugin;

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
}
