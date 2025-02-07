package org.yuezhikong.Server.plugin;

import java.net.URL;
import java.net.URLClassLoader;

public class LibraryClassLoader extends URLClassLoader {
    public LibraryClassLoader(URL[] urls) {
        super(urls);
    } // 此类用于规避 ClassLoader#loadClass 的protected限制

    // 此方法用于规避 ClassLoader#loadClass 的protected限制
    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return super.loadClass(name, resolve);
    }
}
