package com.nettydemo.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Hashtable;
import java.util.Iterator;

public class NioChatServer {

    public static void main(String[] args) {
        // 客户端列表
        Hashtable<String,SocketChannel> clientTable = new Hashtable<>();
        Selector selector = null;
        ServerSocketChannel server = null;
        try {
            // 创建一个选择器
            selector = Selector.open();
            // 创建一个socket
            server = ServerSocketChannel.open();
            server.configureBlocking(false);
            // socket注册到选择器
            server.register(selector, SelectionKey.OP_ACCEPT);
            // 监听端口
            InetSocketAddress ip = new InetSocketAddress(12306);
            server.socket().bind(ip);
            System.err.println("12306端口服务启动成功");
            // 监听事件
            while (true) {
                // 监听事件
                selector.select();
                // 事件列表
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    // 获取事件
                    SelectionKey key = it.next();
                    // 去除事件
                    it.remove();
                    // 事件判断处理
                    if (key.isAcceptable()) {
                        // 连接事件
                        ServerSocketChannel serverSocketChannel = (ServerSocketChannel)key.channel();
                        // 获取接入通道
                        SocketChannel channel = serverSocketChannel.accept();
                        channel.configureBlocking(false);
                        // 判断是否连接
                        if (channel.isConnectionPending()) {
                            // 连接完成
                            channel.finishConnect();
                        }
                        // 注册
                        channel.register(selector,SelectionKey.OP_READ);
                        System.err.println("客户端连接：" + channel.socket().getInetAddress().getHostName() + ":" + channel.socket().getPort());
                    } else if (key.isReadable()) {
                        // 读取事件
                        SocketChannel socketChannel = (SocketChannel)key.channel();
                        CharsetDecoder decoder = Charset.forName("utf-8").newDecoder();
                        ByteBuffer byteBuffer = ByteBuffer.allocate(512);
                        socketChannel.read(byteBuffer);
                        byteBuffer.flip();
                        String msg = decoder.decode(byteBuffer).toString();
                        System.err.println("收到的消息：" + msg);
                        if (msg.startsWith("username=")) {
                            String username = msg.replaceAll("username=","");
                            clientTable.put(username,socketChannel);
                        } else {
                            // 转发消息给客户端
                            String[] arr = msg.split(":");
                            if (arr.length == 3) {
                                // 发送者
                                String from = arr[0];
                                // 接收者
                                String to = arr[1];
                                // 发送内容
                                String content = arr[2];
                                if (clientTable.containsKey(to)) {
                                    CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();
                                    clientTable.get(to).write(encoder.encode(CharBuffer.wrap(from + "】" + content)));
                                }
                            } else {
                                // 发送者
                                String from = arr[0];
                                // 发送内容
                                String content = "来自服务器消息：您未指定接收人";
                                CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();
                                //给接收者发送消息
                                clientTable.get(from).write(encoder.encode(CharBuffer.wrap(content)));
                            }
                        }
                    }
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }finally {
            try {
                selector.close();
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
