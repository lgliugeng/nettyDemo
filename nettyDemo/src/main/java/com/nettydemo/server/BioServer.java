package com.nettydemo.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class BioServer {


    public static void main(String[] args) {
        server5Nio();
    }

    // 创建一个接收字节数组
    static byte[] bt = new byte[1024];
    /**传统Bio编程服务端但会导致连接读取受阻*/
    public static void server(){
        try {
            // 创建一个ServerSocket监听
            ServerSocket server = new ServerSocket(8888);
            while (true) {
                System.out.println("wait coon");
                // 监听连接（阻塞监听，放弃cpu）并返回连接对象，若前面的连接正在连接，导致后面的连接无法接入，
                // 而后面的连接接入时将覆盖前面的连接，导致数据丢失，因此需要启用线程并记录连接对象
                // nio的改进便是对此处进行非阻塞并通过selector记录监听连接对象
                Socket socket = server.accept();
                System.out.println("coon success");
                // 读取传输的数据（阻塞读取）每次只能读取一个连接，其余连接等待读取，若连接不操作，将阻塞其他连接的写，因此这里需要加多线程
                // nio的改进便是对此处进行非阻塞
                System.out.println("wait read");
                socket.getInputStream().read(bt);
                // 输出内容
                System.out.println(new String(bt));
                System.out.println("read success");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**传统Bio编程服务端增加多线程解决连接读取受阻*/
    public static void server2(){
        try {
            // 创建一个ServerSocket监听
            ServerSocket server = new ServerSocket(8888);
            // 多线程获取连接
            new Thread(()->{
                while (true){
                    try {
                        System.out.println("wait coon");
                        // 监听连接（阻塞监听，放弃cpu）并返回连接对象，由于加入多线程，此处不阻塞，每个连接占用一个线程，若线程都处于闲时，导致资源浪费
                        // nio的改进便是对此处进行非阻塞并通过selector记录监听连接对象使用单线程进行处理，将不会出现资源的浪费和占用
                        Socket socket = server.accept();
                        System.out.println("coon success");
                        // 多线程等待连接读取
                        new Thread(()->{
                            try {
                                // 读取传输的数据（阻塞读取）每次只能读取一个连接，其余连接等待读取，若连接不操作，将阻塞其他连接，因此这里需要加多线程
                                // nio的改进便是对此处进行非阻塞
                                System.out.println("wait read");
                                byte[] btN = new byte[1024];
                                InputStream inputStream = socket.getInputStream();
                                while (true){
                                    int len;
                                    if ((len = inputStream.read(btN)) != -1) {
                                        // 输出内容
                                        System.out.println(new String(btN));
                                    }else if((len = inputStream.read(btN)) == -1){
                                        System.out.println("close one thread");
                                        socket.close();
                                        break;
                                    }
                                }
                            }catch (IOException e){
                                e.printStackTrace();
                            }
                            System.out.println("conn close");
                        }).start();
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**传统Bio编程服务端模仿nio原理单线程执行*/
    public static void server3(){
        // 客户端连接对象
        List<Socket> clientList = new ArrayList<>();
        try {
            // 创建一个ServerSocket监听
            ServerSocket server = new ServerSocket(8888);
            try {
                while (true) {
                    // 获取对象
                    AtomicInteger count = new AtomicInteger(clientList.size());
                    for (Socket socket : clientList) {
                        new Thread(()->{
                            synchronized (socket){
                                System.out.println("preparing read");
                                try {
                                    // 读取传输的数据（阻塞读取）每次只能读取一个连接，其余连接等待读取，若连接不操作，将阻塞其他连接，因此这里需要加多线程
                                    // nio的改进便是对此处进行非阻塞
                                    System.out.println("wait read");
                                    byte[] btN = new byte[1024];
                                    InputStream inputStream = socket.getInputStream();
                                    try {
                                        int len;
                                        if ((len = inputStream.read(btN)) != -1) {
                                            // 输出内容
                                            System.out.println(new String(btN));
                                        }else if((len = inputStream.read(btN)) == -1){
                                            System.out.println("close one thread");
                                            socket.close();
                                            clientList.remove(socket);
                                            int t = count.get();
                                            count.set(t-1);
                                        }
                                    }catch (Exception e){
                                        System.out.println("exception close one thread");
                                        socket.close();
                                        clientList.remove(socket);
                                        int t = count.get();
                                        count.set(t-1);
                                    }
                                }catch (IOException e){
                                    e.printStackTrace();
                                }
                            }
                        }).start();
                    }
                    Thread.sleep(1000);
                    System.out.println("读取数据后，此时连接数"+count);
                    System.out.println("wait coon");
                    // 监听连接（阻塞监听，放弃cpu）并返回连接对象，加入到连接列表，不阻塞
                    // nio的改进便是对此处进行非阻塞并通过selector记录监听连接对象使用单线程进行处理，将不会出现资源的浪费和占用
                    new Thread(()->{
                        try {
                            synchronized (clientList){
                                Socket socket = server.accept();
                                if (socket != null) {
                                    // 添加到客户端连接列表a
                                    clientList.add(socket);
                                    System.out.println("coon success");
                                }else{
                                    System.out.println("no coon");
                                }
                                System.out.println("加入连接后，此时连接数"+clientList.size());
                            }
                        }catch (Exception e){

                        }
                    }).start();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**nio异步监控*/
    public static void server4Nio(){
        try {
            // 获取server
            ServerSocketChannel server = ServerSocketChannel.open();
            InetSocketAddress ip = new InetSocketAddress("192.168.13.68",8888);
            // 监听端口
            server.bind(ip);
            // 异步
            server.configureBlocking(false);
            // 客户端连接对象
            List<SocketChannel> clientList = new ArrayList<>();
            while (true){
                Iterator<SocketChannel> it = clientList.iterator();
                while (it.hasNext()){
                    SocketChannel socketChannel = it.next();
                    try {
                        System.err.println("是否连接："+socketChannel.isConnected());
                        ByteBuffer bf =ByteBuffer.allocate(512);
                        int read = socketChannel.read(bf);
                        if (read > 0) {
                            bf.flip();
                            System.out.println(new String(bf.array()));
                        }else if (read == -1){
                            it.remove();
                            socketChannel.close();
                            System.out.println("close socket");
                        }
                    }catch (Exception e){
                        it.remove();
                        socketChannel.close();
                        System.out.println("exception close socket");
                    }
                }
                System.out.println("读取数据后，此时连接数"+clientList.size());
                // 进行监听客户端
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("preparing listen");
                SocketChannel clientSocket = server.accept();
                if (clientSocket != null) {
                    // 客户端异步读取数据
                    clientSocket.configureBlocking(false);
                    // 加入连接列表
                    clientList.add(clientSocket);
                    System.out.println("加入连接后，此时连接数"+clientList.size());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**nio单线程处理*/
    public static void server5Nio(){
        try {
            // 创建一个服务端选择器进行监听连接管理
            Selector serverSlector = Selector.open();
            // 创建一个客户端选择器进行读写事件管理
            Selector clientSelector = Selector.open();
            // 一个线程专门进行监听
            new Thread(()->{
                try {
                    // 获取server
                    ServerSocketChannel server = ServerSocketChannel.open();
                    InetSocketAddress ip = new InetSocketAddress("192.168.13.68",8888);
                    // 监听端口
                    server.bind(ip);
                    // 异步
                    server.configureBlocking(false);
                    // socket服务注册到选择器
                    server.register(serverSlector, SelectionKey.OP_ACCEPT);
                    while (true) {
                        // 监听事件
                        if (serverSlector.select(1) > 0) {
                            Iterator<SelectionKey> it = serverSlector.selectedKeys().iterator();
                            System.out.println("accept listen success");
                            while (it.hasNext()) {
                                SelectionKey key = it.next();
                                if(key.isAcceptable()) {
                                    try {
                                        System.out.println("preparing coon");
                                        // 连接事件
                                        ServerSocketChannel serverSocketChannel = (ServerSocketChannel)key.channel();
                                        // 获取接入通道
                                        SocketChannel channel = serverSocketChannel.accept();
                                        // 异步
                                        channel.configureBlocking(false);
                                        // 判断是否连接
                                        if (channel.isConnectionPending()) {
                                            // 连接完成
                                            channel.finishConnect();
                                        }
                                        // 注册到客户端读写事件管理中
                                        channel.register(clientSelector,SelectionKey.OP_READ);
                                        System.out.println("conn success");
                                    }catch (Exception e){
                                        e.printStackTrace();
                                    }finally {
                                         // 去除事件,否则还会存有上个事件
                                         it.remove();
                                    }
                                }
                            }
                        }
                    }
                }catch (Exception e){

                }
            }).start();
            // 一个线程专门处理读写事件
            new Thread(()->{
                //读写事件
                while (true) {
                    try {
                        if (clientSelector.select(1) > 0) {
                            Iterator<SelectionKey> it = clientSelector.selectedKeys().iterator();
                            System.out.println("readwrite listen success");
                            while (it.hasNext()) {
                                SelectionKey key = it.next();
                                if(key.isReadable()) {
                                    System.out.println("preparing read");
                                    SocketChannel socketChannel = (SocketChannel)key.channel();
                                    try {
                                        // 读取事件
                                        socketChannel.configureBlocking(false);
                                        ByteBuffer bf =ByteBuffer.allocate(512);
                                        int read = socketChannel.read(bf);
                                        if (read > 0) {
                                            bf.flip();
                                            System.out.println(new String(bf.array()));
                                        } else if (read == -1) {
                                            socketChannel.close();
                                        }
                                    }catch (Exception e){
                                        e.printStackTrace();
                                        socketChannel.close();
                                    }finally {
                                        // 去除事件,否则还会存有上个事件
                                        it.remove();
                                    }
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
