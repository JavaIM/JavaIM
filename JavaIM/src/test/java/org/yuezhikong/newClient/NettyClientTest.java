package org.yuezhikong.newClient;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.junit.jupiter.api.Test;
import org.yuezhikong.utils.Logger;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class NettyClientTest {
    @Test
    void start() {
        //连接测试
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.DEBUG))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel channel) {
                        ChannelPipeline pipeline = channel.pipeline();
                        pipeline.addLast(new StringDecoder(StandardCharsets.UTF_8));//IO
                        pipeline.addLast(new StringEncoder(StandardCharsets.UTF_8));
                        pipeline.addLast(new LineBasedFrameDecoder(100000000));
                        pipeline.addLast(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                ctx.channel().close();
                                channel.close();
                                bossGroup.shutdownGracefully();
                                workerGroup.shutdownGracefully();
                            }
                        });
                    }
                });
        try {
            ChannelFuture future = null;
            Random random = new Random();
            boolean bindSuccess = false;
            int port = 0;
            do {
                try {
                    port = random.nextInt(65535);
                    ServerSocket serverSocket = new ServerSocket(port);
                    serverSocket.close();
                    future = bootstrap.bind(port);
                    bindSuccess = true;
                } catch (IOException ignored) {
                }
            } while (!bindSuccess);
            assertNotNull(future);
            NettyClient nettyClient = NettyClient.getInstance();
            nettyClient.start("localhost", port, "a");
            ChannelFuture finalFuture = future;
            assertDoesNotThrow(() -> finalFuture.channel().closeFuture().await(10000));
            assertTrue(future.channel().closeFuture().isSuccess());

        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            if (NettyClient.getInstance().isStarted())
                NettyClient.getInstance().stop();
        }
    }

    @Test
    void sendMessage() {
        EmbeddedChannel channel = new EmbeddedChannel(new StringDecoder(StandardCharsets.UTF_8), new StringEncoder(StandardCharsets.UTF_8));
        NettyClient nettyClient = NettyClient.getInstance();
        assertDoesNotThrow(() -> {
            Field ChannelField = NettyClient.class.getDeclaredField("channel");
            ChannelField.setAccessible(true);
            ChannelField.set(nettyClient, channel);
            ChannelField.setAccessible(false);

            Field StartStatus = NettyClient.class.getDeclaredField("started");
            StartStatus.setAccessible(true);
            StartStatus.set(nettyClient, true);
            StartStatus.setAccessible(false);

            ExecutorService service = Executors.newSingleThreadExecutor();
            Field UserRequestDisposeThreadPool = NettyClient.class.getDeclaredField("UserRequestDisposeThreadPool");
            UserRequestDisposeThreadPool.setAccessible(true);
            UserRequestDisposeThreadPool.set(nettyClient, service);
            UserRequestDisposeThreadPool.setAccessible(false);

            Field ClientStatus = NettyClient.class.getDeclaredField("status");
            ClientStatus.setAccessible(true);
            ClientStatus.set(nettyClient, new NettyClient.ClientStatus(NettyClient.EncryptionMode.NON_ENCRYPTION,null));
            ClientStatus.setAccessible(false);

            nettyClient.sendMessage("test");
            Object recvData = channel.readOutbound();
            assertNotNull(recvData);
            assertTrue(recvData instanceof ByteBuf);
            Logger logger = new Logger(null);
            logger.info("接收到消息！");
        });
    }


    @Test
    void commandRequest() {
        NettyClient client = NettyClient.getInstance();
        assertDoesNotThrow(()-> {
            Field logger = NettyClient.class.getDeclaredField("logger");
            logger.setAccessible(true);
            logger.set(client, new Logger(null));
            logger.setAccessible(false);

            assertTrue(client.CommandRequest(".help",
                    new NettyClient.ClientStatus(NettyClient.EncryptionMode.NON_ENCRYPTION,null),null));

            assertThrows(IllegalArgumentException.class,() -> client.CommandRequest("",
                    new NettyClient.ClientStatus(NettyClient.EncryptionMode.NON_ENCRYPTION,null),null));
        });
    }
}