package com.nettydemo.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Iterator;

public class NioClientThread extends Thread {

    private CharsetDecoder decoder = Charset.forName("utf-8").newDecoder();

    private CharsetEncoder encoder = Charset.forName("utf-8").newEncoder();

    private Selector selector = null;

    private SocketChannel socketChannel = null;

    private SelectionKey clientKey = null;

    private String username;

    public NioClientThread(String username){
        try {
            // 创建选择器
            selector = Selector.open();
            // 创建socket
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            // 注册到选择器
            clientKey = socketChannel.register(selector,SelectionKey.OP_CONNECT);
            // 远程连接绑定
            InetSocketAddress ip = new InetSocketAddress("192.168.13.68",12306);
            socketChannel.connect(ip);
            this.username = username;
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            // 监听事件
            while (true) {
                selector.select(1);
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove();
                    if (key.isConnectable()) {
                        // 连接事件
                        SocketChannel channel = (SocketChannel)key.channel();
                        // 完成连接
                        if (channel.isConnectionPending()) {
                            channel.finishConnect();
                        }
                        // 注册到选择器为读事件
                        channel.register(selector,SelectionKey.OP_READ);
                        System.err.println("服务端连接成功");
                        // 发送用户名
                        send("username=" + username);
                    } else if (key.isReadable()) {
                        // 读取事件
                        SocketChannel channel = (SocketChannel)key.channel();
                        // 读取数据
                        ByteBuffer byteBuffer = ByteBuffer.allocate(512);
                        channel.read(byteBuffer);
                        byteBuffer.flip();
                        String msg = decoder.decode(byteBuffer).toString();
                        System.err.println("【收到信息：" + msg);
                    }
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    void send(String msg) {
        try {
            SocketChannel channel = (SocketChannel)clientKey.channel();
            channel.write(encoder.encode(CharBuffer.wrap(msg)));
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public void close(){
        try {
            selector.close();
            socketChannel.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
