# 📝 服务端配置

## 如果您要配置一个服务端，请进行以下步骤

## 第零步 —— 安装JDK(Java Development Kits)

此处以JDK 19为例，访问https://jdk.java.net/19/ 后，在Build部分，根据您的系统版本，选择，并进行下载

解压缩后，将jdk19.0.2的路径加入系统环境变量：JAVA\_HOME，并且在系统Path中，加入%JAVA\_HOME%\bin

## 第一步 —— 安装git

访问https://git-scm.com/downloads 并且根据您的系统版本，选择并进行下载，下载后进行安装

## 第二步 —— 安装Maven

下载https://dlcdn.apache.org/maven/maven-3/3.9.0/binaries/apache-maven-3.9.0-bin.zip 后，解压，并将解压后的文件夹的路径加入系统环境变量：MAVEN\_HOME，并且在系统Path中，加入%MAVEN\_HOME%\bin

## 第三步 —— 克隆仓库

下载并安装 git 后，创建一个新文件夹，输入以下命令：git clone https://github.com/QiLechan/JavaIM.git

等待跑完后，访问出现的新文件夹

## 第四步 —— 修改配置文件

返回git克隆后出现的新文件夹，并，访问下面的src/main/java/org/yuezhikong

找到本文件夹中的“config.java”，使用富文本编辑器（如果是Windows用户，一定不能使用记事本！），根据您的需要，进行更改

一般只需要更改MySQLDataBaseHost、MySQLDataBasePort、MySQLDataBaseName、MySQLDataBaseUser、MySQLDataBasePasswd为您的mysql相关即可

## 第五步 —— 正式构建

再次git 克隆出现的新文件夹 打开终端(cmd) Linux用户请使用

```bash
chmod 777 ./build.sh
./build.sh
cd target
java -Dfile-encoding=UTF-8 -jar JavaIM-1.0-SNAPSHOT.jar
```

后续使用直接

```bash
cd target
java -Dfile-encoding=UTF-8 -jar JavaIM-1.0-SNAPSHOT.jar
```

Windows用户请使用

```cmd
build.bat
cd target
java -Dfile-encoding=UTF-8 -jar JavaIM-1.0-SNAPSHOT.jar
```

后续使用直接

```cmd
cd target
java -Dfile-encoding=UTF-8 -jar JavaIM-1.0-SNAPSHOT.jar
```

## 第六步 —— 根据程序提示配置

程序会进行提示，根据这些提示，进行配置

## 第七步 —— 发送生成的Public.key给您的用户

服务端配置已经完成了，接下来是将服务端的公钥发给客户端，否则他们将无法加入聊天

这一步，是最后一步了

您在打开服务端后，寻找服务端的运行目录，一般就是git克隆后的文件夹下的target文件夹，在这里找到“Public.key”，并将它发给您的用户

且让您的用户收到后，将他改名为ServerPublicKey.key然后放到他的客户端的运行目录

## 第八步 —— 完成

很好！到了这里，你应该就可以进行聊天了！
