---
description: This page is about how to deploy a JavaIM server
---

# 🛠 Start Server

## Use the built jar package

### Step 1. Install Java

JavaIM requires Java 17. You need to go to Oracle’s official website to download the installation package of JDK 17.

Click [here](https://download.oracle.com/java/17/latest/jdk-17\_windows-x64\_bin.exe) to download the installation package of JDK 17.

### Step 2. Get JavaIM

The JavaIM client and server are merged into one program. Please go to the official JavaIM repository to download the latest [releases](https://github.com/JavaIM/JavaIM/releases).

### Step 3. Run JavaIM

JavaIM has two startup modes: GUI startup and command line startup.

To start the GUI, simply double-click to open it.

If you want to use the command line, please use "java -jar JavaIM.jar" to start it.

If you are using Windows Server Core, please add "nogui" after the startup command, such as "java -jar JavaIM.jar nogui", and then follow the prompts.

### Step 4. Configure

When JavaIM is run for the first time, it will generate two configuration files and two keys in the running directory. The configuration files are for the client and server respectively. Please send the public key to users who need to use the client.&#x20;

As a server, you only need to edit the ‘server.properties’ file.

A standard properties file should look like this:

```properties
MySQLDataBaseHost=127.0.0.1 // MySQL database address
MySQLDataBaseName=JavaIM    // MySQL database name
MAX_CLIENT=-1               // Maximum number of clients that can be connected, -1 means no limit
Use_SQLITE_Mode=true        // Whether to use SQLite
MySQLDataBaseUser=JavaIM    // MySQL database username
MySQLDataBasePort=3306      // MySQL database port
EnableLoginSystem=true      // Whether to enable login system
MySQLDataBasePasswd=JavaIM  // MySQL database password
```

## Install by Docker

This part is still under development.

## Build

If you want to build JavaIM yourself, please install Maven, configure the Java development environment, pull the JavaIM repository, and run “build.bat” or “build.sh”.

Note that the code in the repository is usually the latest and has not been extensively tested. There may be a lot of bugs and it is not recommended for production use.
