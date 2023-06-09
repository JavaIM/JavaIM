---
description: 本页是关于如何部署一个JavaIM服务端
---

# 📝 服务端配置

## 使用已经构建完成的Jar包

### 一、安装Java

JavaIM的最低要求为Java17，你需要前往Oracle的官方网站下载JDK17的安装文件。

点击[此处](https://download.oracle.com/java/17/latest/jdk-17\_windows-x64\_bin.exe)下载JDK17的Windows安装包

### 二、获取JavaIM

JavaIM的客户端与服务端合并在一个程序中，请前往JavaIM的官方仓库下载最新的[releases](https://github.com/JavaIM/JavaIM/releases)

### 三、运行JavaIM

JavaIM有两种启动方式：GUI启动和命令行启动。

GUI启动只需要双击打开即可。

如使用命令行，请使用“java -jar JavaIM.jar -nogui”启动。

请注意，这将会覆盖您在配置文件中的“GUIMode”配置项

如需了解更多关于命令行参数的信息，请使用
“java -jar JavaIM.jar -help”

之后根据提示操作。

### 四、配置JavaIM

在首次运行时，JavaIM将在运行目录生成两个配置文件和两个密钥，配置文件分别是用于客户端和服务端，公钥请发送给需要使用客户端的用户。

作为服务端，只需要编辑“server.properties”文件即可。

一个标准的配置文件应该是这样：

```properties
GUIMode=true                // 是否使用GUI模式
MySQLDataBaseHost=127.0.0.1 // MySQL数据库地址
MySQLDataBaseName=JavaIM    // MySQL数据库名
MAX_CLIENT=-1               // 可连接的最大客户端数量，-1为不限制
Use_SQLITE_Mode=true        // 是否使用SQLite
MySQLDataBaseUser=JavaIM    // MySQL数据库用户名
MySQLDataBasePort=3306      // MySQL数据库端口
EnableLoginSystem=true      // 是否启用登录系统
MySQLDataBasePasswd=JavaIM  // MySQL数据库密码
```

## Docker安装

此部分的内容仍然处于开发阶段

## 自行构建

如果您希望自己构建JavaIM使用，请安装好Maven，配置完成Java开发环境，拉取JavaIM的仓库，运行“build.bat”或“build.sh”。

注意，仓库中的代码通常是最新的，没有经过大量测试的，可能会出现大量Bug，不建议生产环境使用。

目前预计将于2023年6月17日开始对于JavaIM的重构，以解决绝大多数的Bug
