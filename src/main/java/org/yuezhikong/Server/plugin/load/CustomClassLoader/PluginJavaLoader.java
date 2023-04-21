package org.yuezhikong.Server.plugin.load.CustomClassLoader;

import org.yuezhikong.Server.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;

public final class PluginJavaLoader extends URLClassLoader {
    public final Plugin ThisPlugin;
    public PluginJavaLoader(ClassLoader parent, File s) throws IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        super(new URL[]{s.toURI().toURL()}, parent);
        InputStream propertiesStream = this.getResourceAsStream("PluginManifest.properties");
        Properties properties = new Properties();
        properties.load(propertiesStream);
        String mainClass = properties.getProperty("Main-Class");
        Class<?> jarClass = Class.forName(mainClass,true,this);
        Class<? extends Plugin> pluginClass = jarClass.asSubclass(Plugin.class);
        ThisPlugin = pluginClass.getDeclaredConstructor().newInstance();
    }
}