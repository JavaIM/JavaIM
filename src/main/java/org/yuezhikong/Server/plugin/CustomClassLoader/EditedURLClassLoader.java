package org.yuezhikong.Server.plugin.CustomClassLoader;

import org.yuezhikong.Main;
import org.yuezhikong.Server.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;

public final class EditedURLClassLoader extends URLClassLoader {
    public final Plugin ThisPlugin;
    public EditedURLClassLoader(ClassLoader parent, File s) throws IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        super(new URL[]{s.toURI().toURL()}, parent);
        this.addURL(Main.class.getProtectionDomain().getCodeSource().getLocation());
        InputStream propertiesStream = this.getResourceAsStream("PluginManifest.properties");
        Properties properties = new Properties();
        properties.load(propertiesStream);
        String mainClass = properties.getProperty("Main-Class");
        Class<?> jarClass = Class.forName(mainClass,true,this);
        Class<? extends Plugin> pluginClass = jarClass.asSubclass(Plugin.class);
        ThisPlugin = pluginClass.getDeclaredConstructor().newInstance();
    }
}
