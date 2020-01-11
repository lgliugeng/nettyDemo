package com.nettydemo.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class NettyClient3 {

    public static void main(String[] args) throws Exception {
        NettyClient3 client = new NettyClient3("127.0.0.1",8888);
        //启动client服务
        client.start();

        Channel channel = client.getChannel();
        //消息体
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        while(true){
            /*RpcRequest request = new RpcRequest();
            request.setId(UUID.randomUUID().toString());

            request.setData(input.readLine()+"\n");*/
            //channel对象可保存在map中，供其它地方发送消息
            channel.writeAndFlush(input.readLine());
        }
    }

    private final String host;

    private final int port;

    private Channel channel;

    // 连接服务端的端口号地址和端口号

    public NettyClient3(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() throws Exception {
        final EventLoopGroup group = new NioEventLoopGroup();

        Bootstrap b = new Bootstrap();
        // 使用NioSocketChannel来作为连接用的channel类
        b.group(group).channel(NioSocketChannel.class)
                // 绑定连接初始化器
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        System.out.println("客户端开始初始化......");
                        ChannelPipeline pipeline = ch.pipeline();
                        //pipeline.addLast(new RpcEncoder(RpcRequest.class)); //编码request
                        pipeline.addLast(new StringEncoder());
                        //pipeline.addLast(new RpcDecoder(RpcResponse.class)); //解码response
                        pipeline.addLast(new StringDecoder());
                        pipeline.addLast(new ClientHandler()); //客户端处理类

                    }
                });
        //发起异步连接请求，绑定连接端口和host信息
        final ChannelFuture future = b.connect(host, port).sync();

        future.addListener(new ChannelFutureListener() {

            @Override
            public void operationComplete(ChannelFuture arg0) throws Exception {
                if (future.isSuccess()) {
                    System.out.println("连接服务器成功");

                } else {
                    System.out.println("连接服务器失败");
                    future.cause().printStackTrace();
                    group.shutdownGracefully(); //关闭线程组
                }
            }
        });

        this.channel = future.channel();
    }

    public Channel getChannel() {
        return channel;
    }
}
