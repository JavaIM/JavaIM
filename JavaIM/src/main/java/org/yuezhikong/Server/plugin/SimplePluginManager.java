/*
 * Simplified Chinese (简体中文)
 *
 * 版权所有 (C) 2023 QiLechan <qilechan@outlook.com> 和本程序的贡献者
 *
 * 本程序是自由软件：你可以再分发之和/或依照由自由软件基金会发布的 GNU 通用公共许可证修改之，无论是版本 3 许可证，还是 3 任何以后版都可以。
 * 发布该程序是希望它能有用，但是并无保障;甚至连可销售和符合某个特定的目的都不保证。请参看 GNU 通用公共许可证，了解详情。
 * 你应该随程序获得一份 GNU 通用公共许可证的副本。如果没有，请看 <https://www.gnu.org/licenses/>。
 * English (英语)
 *
 * Copyright (C) 2023 QiLechan <qilechan@outlook.com> and contributors to this program
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or 3 any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package org.yuezhikong.Server.plugin;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;
import org.yuezhikong.Server.IServer;
import org.yuezhikong.Server.UserData.user;
import org.yuezhikong.Server.plugin.Plugin.Plugin;
import org.yuezhikong.Server.plugin.Plugin.PluginData;
import org.yuezhikong.Server.plugin.command.CommandExecutor;
import org.yuezhikong.Server.plugin.event.EventHandler;
import org.yuezhikong.Server.plugin.event.Listener;
import org.yuezhikong.Server.plugin.event.events.Event;
import org.yuezhikong.utils.CustomVar;
import org.yuezhikong.utils.SaveStackTrace;
import org.yuezhikong.utils.checks;
import org.yuezhikong.utils.logging.PluginLoggingBridge;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class SimplePluginManager implements PluginManager{
    
    private final IServer serverInstance;
    public SimplePluginManager(IServer ServerInstance) {
        serverInstance = ServerInstance;
    }
    private final List<Plugin> pluginList = new CopyOnWriteArrayList<>();
    private final List<PluginData> pluginDataList = new CopyOnWriteArrayList<>();
    @Override
    public void PreLoadPlugin(@NotNull File PluginFile) throws IOException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, ClassNotFoundException,UnsupportedOperationException {
        //启动classloader
        PluginClassLoader classLoader = new PluginClassLoader(new URL[]{
                PluginFile.toURI().toURL()
        }, ClassLoader.getSystemClassLoader(),this);
        try {
            //读取插件清单
            Properties properties = new Properties();
            properties.load(classLoader.getResourceAsStream("PluginManifest.properties"));//打开插件清单文件
            if (Boolean.parseBoolean(properties.getProperty("doNotHotLoad","false")) && serverInstance.isServerCompleateStart())
                throw new UnsupportedOperationException("This plugin unsupported hot load");
            final String Name = properties.getProperty("Plugin-Name");
            final String Version = properties.getProperty("Plugin-Version");
            final String Author = properties.getProperty("Plugin-Author");
            if (Name == null || Version == null || Author == null)
            {
                LoggerFactory.getLogger(SimplePluginManager.class).error("文件："+PluginFile.getName()+"加载失败，插件清单文件错误");
                return;
            }
            LoggerFactory.getLogger(SimplePluginManager.class).info("正在加载插件 " + Name);
            for (Plugin plugin : pluginList)
            {
                PluginData data = plugin.getPluginData();
                if (Name.equals(data.getStaticData().PluginName()))
                {
                    LoggerFactory.getLogger(SimplePluginManager.class).info("无法加载插件 " + Name+" 因为已安装了同名插件");
                    return;
                }
            }
            //获取插件主类的Class
            Class<?> jarClass = Class.forName(properties.getProperty("Main-Class"), true, classLoader);
            //检测插件主类是否实现了Plugin接口
            Class<? extends Plugin> pluginClass = jarClass.asSubclass(Plugin.class);
            //调用插件构造器并获取插件
            final Plugin plugin = pluginClass.getDeclaredConstructor().newInstance();
            //写入插件的信息到PluginData
            PluginData.staticData staticData = new PluginData.staticData(plugin, Name, Version, Author, classLoader, PluginFile ,false);
            PluginData pluginData = new PluginData(staticData);
            //设定PluginData
            plugin.setPluginData(pluginData);
            if (plugin.getPluginData() == null) {
                LoggerFactory.getLogger(SimplePluginManager.class).info("无法加载插件 " + Name+ " 因为他的getPluginData方法返回null");
                return;
            }
            //调用插件的onPreload方法
            plugin.onPreload();
            //保存插件到插件列表
            pluginList.add(plugin);
            pluginDataList.add(pluginData);
            LoggerFactory.getLogger(SimplePluginManager.class).info("插件 " + Name + "预加载成功");
        }
        catch (Throwable e)
        {
            LoggerFactory.getLogger(SimplePluginManager.class).error("插件加载失败："+PluginFile.getName());
            SaveStackTrace.saveStackTrace(e);
            try {
                classLoader.close();
            } catch (IOException ignored) {}
            if (e instanceof IOException
                    || e instanceof NoSuchMethodException || e instanceof InvocationTargetException
                    || e instanceof InstantiationException || e instanceof IllegalAccessException
                    || e instanceof ClassNotFoundException || e instanceof UnsupportedOperationException)
                throw e;
            else
                throw new RuntimeException("Unexpected Error",e);
        }
    }

    @Override
    public void LoadPlugin(@NotNull File PluginFile) throws IOException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, ClassNotFoundException, UnsupportedOperationException {
        AtomicReference<Plugin> plugin = new AtomicReference<>();
        pluginDataList.forEach(pluginData -> {
           if (pluginData.getStaticData().PluginMainFile().getAbsolutePath().equals(PluginFile.getAbsolutePath()))
               plugin.set(pluginData.getStaticData().plugin());
        });
        if (plugin.get() != null && plugin.get().getPluginData().getStaticData().loaded())//已经加载了
            throw new UnsupportedOperationException("This plugin is already loaded!");
        else if (plugin.get() != null && !plugin.get().getPluginData().getStaticData().loaded()) {//已预加载
            plugin.get().onLoad();
            PluginData.staticData orig_staticData = plugin.get().getPluginData().getStaticData();
            plugin.get().setPluginData(new PluginData(new PluginData.staticData(orig_staticData.plugin(),
                    orig_staticData.PluginName(),
                    orig_staticData.PluginVersion(),
                    orig_staticData.PluginAuthor(),
                    orig_staticData.PluginClassLoader(),
                    orig_staticData.PluginMainFile(),
                    true)));
        }
        else if (plugin.get() == null)//未加载
        {
            throw new RuntimeException("This file is not preLoaded!");
        }
    }

    private record CommandSavedData(String Command, String Description, CommandExecutor executor, Plugin plugin) {}
    private final List<CommandSavedData> commandSavedData = new CopyOnWriteArrayList<>();

    @Override
    public boolean RequestPluginCommand(CustomVar.Command CommandInformation, user User) {
        for (CommandSavedData savedData : commandSavedData)
        {
            if (savedData.Command().equals(CommandInformation.Command()))
            {
                savedData.executor().execute(User,CommandInformation.Command(),CommandInformation.argv());
                return true;
            }
        }
        return false;
    }

    @Override
    public List<String> getPluginCommandsDescription() {
        List<String> ReturnList = new ArrayList<>();
        for (CommandSavedData savedData : commandSavedData)
        {
            ReturnList.add(savedData.Command()+" "+savedData.Description());
        }
        return ReturnList;
    }

    @Override
    public boolean RegisterCommand(String CommandName, String Description, CommandExecutor executor, Plugin plugin) {
        if (CommandName == null || executor == null || plugin == null)
            return false;
        String command = "/"+CommandName;
        for (CommandSavedData savedData : commandSavedData)
        {
            if (savedData.Command().equals(command))
                return false;
        }
        return commandSavedData.add(new CommandSavedData(command,Description,executor,plugin));
    }

    @Override
    public boolean UnRegisterCommand(String CommandName) {
        if (CommandName == null)
            return false;
        return commandSavedData.removeIf(commandSavedData -> commandSavedData.Command().equals("/"+CommandName));
    }

    @Override
    public void AddEventListener(Listener listener, Plugin plugin) {
        plugin.getPluginData().AddEventListener(listener);
    }

    @Override
    public List<Listener> getEventListener(Plugin plugin) {
        return plugin.getPluginData().getEventListener();
    }

    @Override
    public void RemoveEventListener(Listener listener, Plugin plugin) {
        plugin.getPluginData().RemoveEventListener(listener);
    }

    @Override
    public void UnLoadPlugin(@NotNull Plugin plugin) throws IOException {
        checks.checkState(!LoadStage, "Server PreLoad Stage can not Unload Plugin, you can try load in ONLOAD METHOD");
        PluginData pluginData = plugin.getPluginData();
        LoggerFactory.getLogger(SimplePluginManager.class).info("正在卸载插件"+pluginData.getStaticData().PluginName()+
                "v"+pluginData.getStaticData().PluginVersion()+
                " by"+pluginData.getStaticData().PluginAuthor());
        for (Listener listener : plugin.getPluginData().getEventListener())
        {
            RemoveEventListener(listener,plugin);
        }
        try {
            PluginLoggingBridge.RemoveLogger(plugin);
        } catch (IllegalStateException ignored) {}
        pluginData.getStaticData().plugin().onUnload();
        pluginList.remove(plugin);
        pluginDataList.remove(pluginData);
        try {
            pluginData.getStaticData().PluginClassLoader().close();
        } catch (IOException e)
        {
            LoggerFactory.getLogger(SimplePluginManager.class).info("无法卸载插件"+pluginData.getStaticData().PluginName()+
                    "v"+pluginData.getStaticData().PluginVersion()+
                    " by"+pluginData.getStaticData().PluginAuthor());
            throw e;
        }
        LoggerFactory.getLogger(SimplePluginManager.class).info("已卸载插件"+pluginData.getStaticData().PluginName()+
                "v"+pluginData.getStaticData().PluginVersion()+
                " by"+pluginData.getStaticData().PluginAuthor());
    }

    @Override
    public void UnLoadAllPlugin() throws IOException {
        record Data(String Name,String Version,String Author,PluginClassLoader classLoader){}
        List<Data> DataList = new ArrayList<>();
        for (PluginData pluginData : pluginDataList)
        {
            LoggerFactory.getLogger(SimplePluginManager.class).info("正在卸载插件"+pluginData.getStaticData().PluginName()+
                    "v"+pluginData.getStaticData().PluginVersion()+
                    " by"+pluginData.getStaticData().PluginAuthor());
            for (Listener listener : pluginData.getEventListener())
            {
                RemoveEventListener(listener,pluginData.getStaticData().plugin());
            }
            pluginData.getStaticData().plugin().onUnload();
            DataList.add(new Data(pluginData.getStaticData().PluginName(),
                    pluginData.getStaticData().PluginVersion(),
                    pluginData.getStaticData().PluginAuthor(),
                    pluginData.getStaticData().PluginClassLoader()));
        }
        pluginList.clear();
        pluginDataList.clear();
        for (Data data : DataList)
        {
            try {
                data.classLoader().close();
            } catch (IOException e)
            {
                LoggerFactory.getLogger(SimplePluginManager.class).info("无法卸载插件"+data.Name()+
                        "v"+data.Version()+
                        " by"+data.Author());
                DataList.clear();
                throw e;
            }
            LoggerFactory.getLogger(SimplePluginManager.class).info("已卸载插件"+data.Name()+
                    "v"+data.Version()+
                    " by"+data.Author());
        }
        DataList.clear();
    }

    /**
     * 获取文件夹下所有.jar结尾的文件
     * 一般是插件文件
     * @param Directory 文件夹
     * @return 文件列表
     */
    private @Nullable List<File> GetPluginFileList(@NotNull File Directory)
    {
        if (Directory.isDirectory())
        {
            String[] list = Directory.list();
            if (list == null)
            {
                return null;
            }
            List<File> PluginList = new ArrayList<>();
            for (String s : list) {
                if (s.toLowerCase(Locale.ROOT).endsWith(".jar")) {
                    File file1 = new File(Directory.getPath(),s);
                    PluginList.add(file1);
                }
            }
            return PluginList;
        }
        else
            return null;
    }

    private boolean LoadStage = false;
    @Override
    public void PreloadPluginOnDirectory(@NotNull File Directory, ExecutorService StartUpThreadPool) {
        if (!Directory.exists() && !(Directory.mkdir()))
            throw new RuntimeException("创建文件夹失败");

        if (!Directory.isDirectory()) {
            if (!(Directory.delete()))
                throw new RuntimeException("无法删除文件");
            if (!(Directory.mkdir()))
                throw new RuntimeException("创建文件夹失败");
        }

        List<File> fileList = Objects.requireNonNull(GetPluginFileList(Directory));
        if (fileList.isEmpty())
            return;
        List<Future<?>> loadTasksFuture = new ArrayList<>();
        for (File loadPluginFile : fileList) {
            loadTasksFuture.add(StartUpThreadPool.submit(() -> {
                try {
                    PreLoadPlugin(loadPluginFile);
                }
                catch (Throwable t)
                {
                    if (t instanceof ClassCastException)
                        LoggerFactory.getLogger(SimplePluginManager.class).error("文件：{} 加载失败，未实现Plugin接口", loadPluginFile.getName());
                    else if (t instanceof IOException)
                        LoggerFactory.getLogger(SimplePluginManager.class).error("文件：{} 加载失败，出现IO错误", loadPluginFile.getName());
                    else if (t instanceof NoSuchMethodException)
                        LoggerFactory.getLogger(SimplePluginManager.class).error("文件：{} 加载失败，无参构造器不存在", loadPluginFile.getName());
                    else if (t instanceof InvocationTargetException)
                        LoggerFactory.getLogger(SimplePluginManager.class).error("文件：{} 加载失败，插件构造器出现内部错误", loadPluginFile.getName());
                    else if (t instanceof InstantiationException)
                        LoggerFactory.getLogger(SimplePluginManager.class).error("文件：{} 加载失败，主类是抽象方法或接口", loadPluginFile.getName());
                    else if (t instanceof IllegalAccessException)
                        LoggerFactory.getLogger(SimplePluginManager.class).error("文件：{} 加载失败，没有权限访问无参构造器", loadPluginFile.getName());
                    else if (t instanceof ClassNotFoundException)
                        LoggerFactory.getLogger(SimplePluginManager.class).error("文件：{} 加载失败，插件指定的主类不存在", loadPluginFile.getName());
                }
            }));
        }
        for (Future<?> loadTaskFuture : loadTasksFuture) {
            try {
                loadTaskFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("Thread Pool Fatal",e);
            }
        }
    }

    @Override
    public void LoadPluginOnDirectory(@NotNull File Directory, ExecutorService StartUpThreadPool) {
        LoadStage = true;
        if (!Directory.exists() && !(Directory.mkdir()))
            throw new RuntimeException("创建文件夹失败");

        if (!Directory.isDirectory()) {
            if (!(Directory.delete()))
                throw new RuntimeException("无法删除文件");
            if (!(Directory.mkdir()))
                throw new RuntimeException("创建文件夹失败");
        }

        List<File> fileList = Objects.requireNonNull(GetPluginFileList(Directory));
        if (fileList.isEmpty())
            return;
        List<Future<?>> loadTasksFuture = new ArrayList<>();
        for (File loadPluginFile : fileList) {
            loadTasksFuture.add(StartUpThreadPool.submit(() -> {
                try {
                    LoadPlugin(loadPluginFile);
                }
                catch (Throwable t)
                {
                    SaveStackTrace.saveStackTrace(t);
                    if (t instanceof ClassCastException)
                        LoggerFactory.getLogger(SimplePluginManager.class).error("文件：{} 加载失败，未实现Plugin接口", loadPluginFile.getName());
                    else if (t instanceof IOException)
                        LoggerFactory.getLogger(SimplePluginManager.class).error("文件：{} 加载失败，出现IO错误", loadPluginFile.getName());
                    else if (t instanceof NoSuchMethodException)
                        LoggerFactory.getLogger(SimplePluginManager.class).error("文件：{} 加载失败，无参构造器不存在", loadPluginFile.getName());
                    else if (t instanceof InvocationTargetException)
                        LoggerFactory.getLogger(SimplePluginManager.class).error("文件：{} 加载失败，插件构造器出现内部错误", loadPluginFile.getName());
                    else if (t instanceof InstantiationException)
                        LoggerFactory.getLogger(SimplePluginManager.class).error("文件：{} 加载失败，主类是抽象方法或接口", loadPluginFile.getName());
                    else if (t instanceof IllegalAccessException)
                        LoggerFactory.getLogger(SimplePluginManager.class).error("文件：{} 加载失败，没有权限访问无参构造器", loadPluginFile.getName());
                    else if (t instanceof ClassNotFoundException)
                        LoggerFactory.getLogger(SimplePluginManager.class).error("文件：{} 加载失败，插件指定的主类不存在", loadPluginFile.getName());
                }
            }));
        }
        for (Future<?> loadTaskFuture : loadTasksFuture) {
            try {
                loadTaskFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("Thread Pool Fatal",e);
            }
        }
    }

    @Override
    public void callEvent(@NotNull Event event) {
        record MethodData(Method method,Listener listener,PluginData pluginData) {}
        List<MethodData> LowPriorityMethod = new ArrayList<>();
        List<MethodData> NormalPriorityMethod = new ArrayList<>();
        List<MethodData> HighPriorityMethod = new ArrayList<>();
        for (Plugin plugin : pluginList)
        {
            PluginData data = plugin.getPluginData();
            for (Listener listener : data.getEventListener())//获取PluginData中的EventListener后遍历
            {
                Method[] methods = listener.getClass().getDeclaredMethods();//获取方法列表
                for (Method method : methods) {//遍历所有方法
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    if (
                            parameterTypes.length == 1 //判断此方法要求提供的参数是1个
                                    && parameterTypes[0] == event.getClass() //判断此方法是否要求的类型是否与Event一致
                                    && method.isAnnotationPresent(EventHandler.class) //判断是否被EventHandler注解
                    ) {
                        switch (method.getAnnotation(EventHandler.class).Priority()) {//根据优先级拉入对应的List
                            case LOW -> LowPriorityMethod.add(new MethodData(method,listener,data));
                            case NORMAL -> NormalPriorityMethod.add(new MethodData(method,listener,data));
                            case HIGH -> HighPriorityMethod.add(new MethodData(method,listener,data));
                        }
                    }
                }
            }
        }
        //开始调用方法
        List<MethodData> mergedList = new ArrayList<>();
        mergedList.addAll(LowPriorityMethod);
        mergedList.addAll(NormalPriorityMethod);
        mergedList.addAll(HighPriorityMethod);
        for (MethodData methodData : mergedList)//开始调用
        {
            try {
                methodData.method().invoke(methodData.listener(), event);
            } catch (IllegalAccessException e) {
                LoggerFactory.getLogger(SimplePluginManager.class).error("没有权限访问 插件 "+methodData.pluginData().getStaticData().PluginName()+
                        "v"+methodData.pluginData().getStaticData().PluginVersion()+
                        " by "+methodData.pluginData().getStaticData().PluginAuthor()+
                        "的事件处理程序");
            } catch (InvocationTargetException e) {

                SaveStackTrace.saveStackTrace(e);
                LoggerFactory.getLogger(SimplePluginManager.class).error("插件 "+methodData.pluginData().getStaticData().PluginName()+
                        "v"+methodData.pluginData().getStaticData().PluginVersion()+
                        " by "+methodData.pluginData().getStaticData().PluginAuthor()+
                        "的事件处理程序出现内部错误");
            }
        }
    }

    @Override
    public @Nullable Plugin getPluginByName(@NotNull String name) {
        for (Plugin plugin : pluginList)
        {
            if (name.equals(plugin.getPluginData().getStaticData().PluginName()))
            {
                return plugin;
            }
        }
        return null;
    }

    @Override
    public int getPluginNumber() {
        return pluginList.size();
    }

    @Override
    public List<PluginData> getPluginDataList() {
        return new ArrayList<>(pluginDataList);
    }
}
