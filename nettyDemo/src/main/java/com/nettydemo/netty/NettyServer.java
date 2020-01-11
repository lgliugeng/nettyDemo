package com.nettydemo.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.nio.charset.Charset;

public class NettyServer {

    public static void main(String[] args) throws Exception {
        new NettyServer().bind(8888);
    }

    public void bind(int port) throws Exception{
        // 负责处理TCP/IP连接的parentGroup
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        // 负责处理channel的IO事件的childGroup
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup,workerGroup)
                .channel(NioServerSocketChannel.class)
                //初始化服务端可连接队列,指定了队列的大小128
                .option(ChannelOption.SO_BACKLOG,128)
                // 保持长连接
                .childOption(ChannelOption.SO_KEEPALIVE,true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    // 绑定客户端连接时候触发操作
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        System.out.println("用户" + socketChannel.remoteAddress() + "正在连入聊天室...");
                        socketChannel.pipeline()
                                // 解码
                                //.addLast(new RpcDecoder(RpcRequest.class))
                                .addLast(new StringDecoder(Charset.forName("UTF-8")))
                                // 编码
                                //.addLast(new RpcEncoder(RpcResponse.class))
                                .addLast(new StringEncoder(Charset.forName("GBK")))
                                // 使用ServerHandler类来处理接收到的消息
                                .addLast(new ServerHandler());
                    }
                });
        // 绑定监听端口，调用sync同步阻塞方法等待绑定操作完
        ChannelFuture channelFuture = serverBootstrap.bind(port).sync();
        if (channelFuture.isSuccess()) {
            System.out.println("服务端启动成功");
        } else {
            System.out.println("服务端启动失败");
            channelFuture.cause().printStackTrace();
            bossGroup.shutdownGracefully(); //关闭线程组
            workerGroup.shutdownGracefully();
        }

        // 成功绑定到端口之后,给channel增加一个 管道关闭的监听器并同步阻塞,直到channel关闭,线程才会往下执行,结束进程。
        channelFuture.channel().closeFuture().sync();
    }
}
