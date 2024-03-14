package org.yuezhikong.Server.plugin;

import org.yuezhikong.Server.plugin.Plugin.PluginData;

import java.net.URL;
import java.net.URLClassLoader;

public class PluginClassLoader extends URLClassLoader {

    private final PluginManager manager;
    public PluginClassLoader(URL[] urls, ClassLoader parent,PluginManager manager) {
        super(urls, parent);
        this.manager = manager;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        try
        {
            return super.loadClass(name, resolve);
        }
        catch (ClassNotFoundException ignored) {}
        //未在父类找到(意味着JavaIM主程序与插件本身均未找到插件，应试图前去其他插件加载)
        for (PluginData data : manager.getPluginDataList())
        {
            try {
                return data.getStaticData().PluginClassLoader().loadClass(name);
            } catch (ClassNotFoundException ignored) {}
        }
        throw new ClassNotFoundException(name);
    }
}
