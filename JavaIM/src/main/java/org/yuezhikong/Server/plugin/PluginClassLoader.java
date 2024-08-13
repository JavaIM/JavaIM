package org.yuezhikong.Server.plugin;

import org.yuezhikong.Server.plugin.plugin.PluginData;

import java.net.URL;
import java.net.URLClassLoader;

public class PluginClassLoader extends URLClassLoader {

    private final PluginManager manager;

    public PluginClassLoader(URL[] urls, ClassLoader parent, PluginManager manager) {
        super(urls, parent);
        this.manager = manager;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        try {
            return super.loadClass(name, resolve);
        } catch (ClassNotFoundException ignored) {
        }

        if (noRecursion)
            throw new ClassNotFoundException(name);
        //未在父类找到(意味着JavaIM主程序与插件本身均未找到插件，应试图前去其他插件加载)
        for (PluginData data : manager.getPluginDataList()) {
            data.getStaticData().classLoader().setNoRecursion(true);
            try {
                return data.getStaticData().classLoader().loadClass(name);
            } catch (ClassNotFoundException ignored) {
            } finally {
                data.getStaticData().classLoader().setNoRecursion(false);
            }
        }
        throw new ClassNotFoundException(name);
    }

    private boolean noRecursion;

    /**
     * 设置是否禁止递归
     */
    private void setNoRecursion(boolean noRecursion) {
        this.noRecursion = noRecursion;
    }
}
