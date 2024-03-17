# JavaIM

[![status](https://img.shields.io/github/actions/workflow/status/JavaIM/JavaIM/buildandcodeql.yml?style=for-the-badge)](https://github.com/JavaIM/JavaIM/actions)
[
![Latest Tag](https://img.shields.io/github/v/tag/JavaIM/JavaIM?label=LATEST%20TAG&style=for-the-badge)
![GitHub Releases (by Asset)](https://img.shields.io/github/downloads/JavaIM/JavaIM/latest/total?style=for-the-badge)
](https://github.com/QiLechan/JavaIM/releases/latest)  

> 注意： JavaIM仍然处于开发阶段，不能保证通信的安全性。

JavaIM是一款使用Java编写的加密通信软件。

JavaIM目前通过RSA+AES算法对通信内容进行加密。

当前项目正在重构，可能暂时不可用，重构结束后，将会改用SSL

## 对于v1.3.0出现的加解密有时失败的问题的修复
请打开org.yuezhikong.newServer.NettyServer

找到StartChatRoomServerForNetty方法，netty initChannel方法，将其修改为
```java
@Override
public void initChannel(SocketChannel channel) {
    ChannelPipeline pipeline = channel.pipeline();
    pipeline.addLast(new LineBasedFrameDecoder(100000000));
    pipeline.addLast(new StringDecoder(StandardCharsets.UTF_8));//IO
    pipeline.addLast(new StringEncoder(StandardCharsets.UTF_8));
    pipeline.addLast(new ServerInDecoder());
    pipeline.addLast(new ServerOutEncoder());
    pipeline.addLast(RecvMessageThreadPool,new ServerInHandler());//JavaIM逻辑
}
```
重新编译后即可解决问题

## 现在开始！
### 💻 使用JavaIM
请参阅[指南](https://docs.qileoffice.top/start/server-start)以使用JavaIM

### 🎯 部署与配置服务端
请参阅[指南](https://docs.qileoffice.top/start/install/client-start)配置服务端
