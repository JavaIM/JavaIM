# 🛠 客户端配置

## 第零步 —— 安装Java

这里需要至少Java17

由于作者找不到高版本jre，这里以jdk19举例，但，不管是jdk还是jre，都是可以使用的，不过版本至少是Java17

访问https://jdk.java.net/19/ 在Builds中寻找您当前操作系统版本的版本

下载后进行解压，将解压后的路径加入到系统Java\_Home中

## 第一步 —— 从Github Action下载默认配置的JavaIM

使用浏览器访问https://github.com/QiLechan/JavaIM/actions/workflows/maven.yml 点开右侧有绿色对钩的项目的文字，建议是最上面的，但，请注意，后方有一个蓝框，请保证是main，如为其他的，不保证稳定性

在打开的新界面，确认您打开的是Summary页面，查看方法是看左侧，Summary左侧是否有颜色，如果没有，请点击一下Summary

打开Summary后，在新界面寻找Artifacts，您应该可以在他的区域找到"JavaIM请解压后使用"，点击它，您的浏览器应该会开始下载

下载后，解压，将里面的JavaIM-1.0-SNAPSHOT.jar复制到一个文件夹中

## 第二步 —— 从服务端获取公钥

获取公钥后，将他改名为ServerPublicKey.key，并将它和jar放在一起

## 第三步 —— 根据提示进行运行

请在之前的文件夹中打开终端（Windows系统为cmd）
如果是Linux用户，请在这里输入
```bash
$Java_Home/bin/java.exe -Dfile-encoding=UTF-8 -jar JavaIM-1.0-SNAPSHOT.jar
```
如果是windows用户，也请在这里输入
```cmd
%Java_Home%\bin\java.exe -Dfile-encoding=UTF-8 -jar JavaIM-1.0-SNAPSHOT.jar
```
后续根据提示进行输入
如果还想打开，只需再次执行第三步即可

## 第四步 —— 完成

您到了此处后，应该就已经可以开始聊天了
