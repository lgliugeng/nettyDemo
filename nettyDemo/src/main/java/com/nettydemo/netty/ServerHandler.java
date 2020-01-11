package com.nettydemo.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.*;

public class ServerHandler extends ChannelInboundHandlerAdapter{

    // 通道组

    public static final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    // 客户映射列表

    public static final Map<String,Map<String,Integer>> clientMap = new HashMap<>();

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        Channel clientChannel = ctx.channel();
        channels.add(clientChannel);
        channels.forEach(channel -> {
            if (channel != clientChannel) {
                // 返回结构
                /*RpcResponse response = new RpcResponse();
                response.setId(UUID.randomUUID().toString());
                response.setStatus(1);*/
                //response.setData("【提示】：用户【" + clientChannel.remoteAddress() + "】加入聊天室！");
                //channel.writeAndFlush("【提示】：用户【" + clientChannel.remoteAddress() + "】加入聊天室！");
                channel.writeAndFlush("【提示】：用户【" + clientChannel.remoteAddress() + "】加入聊天室！\n");

            }
        });
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        Channel clientChannel = ctx.channel();
        channels.remove(clientChannel);
        channels.forEach(channel -> {
            if (channel != clientChannel) {
                // 返回结构
                /*RpcResponse response = new RpcResponse();
                response.setId(UUID.randomUUID().toString());
                response.setStatus(1);*/
                //response.setData("【提示】：用户【" + clientChannel.remoteAddress() + "】离开聊天室！");
                //channel.writeAndFlush("【提示】：用户【" + clientChannel.remoteAddress() + "】离开聊天室！");
                channel.writeAndFlush("【提示】：用户【" + clientChannel.remoteAddress() + "】离开聊天室！\n");
                //channel.writeAndFlush("remove:" + splitAddress(channel.remoteAddress().toString()));
            }
        });
    }

    // 接受client发送的消息

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel clientChannel = ctx.channel();
        // 用户端口
        String currentAddr = splitAddress(clientChannel.remoteAddress().toString());
        System.out.println(currentAddr);
        if ("用户列表".equals(msg.toString())) {
            // 用户列表
            String clientStr = getClientTables(clientChannel);
            clientChannel.writeAndFlush("用户列表，请使用To:编号选择用户，使用To:All进入群聊：" + clientStr + "\n");
            return;
        }
        // 选择用户，加入映射
        if (msg != null && msg.toString().contains("To:") && !msg.toString().contains("To:全部")){
            String targetAddr = splitAddress(msg.toString());
            Map<String,Integer> targetMap = clientMap.get(currentAddr);
            // 选择私聊时将与其他用户的私聊标识变为0 1-标识当前私聊用户
            if (Objects.nonNull(targetMap) && !targetMap.isEmpty()) {
                Set<String> keys = targetMap.keySet();
                keys.forEach(key->{
                    targetMap.put(key,0);
                });
            } else {
                // 记录当前私聊用户
                Map<String,Integer> tempMap = new HashMap<>();
                tempMap.put(targetAddr,1);
                clientMap.put(currentAddr,tempMap);
                clientChannel.writeAndFlush("您已选择和用户【" + targetAddr + "】私聊！\n");
                return;

            }
            // 记录当前私聊用户
            targetMap.put(targetAddr,1);
            clientMap.put(currentAddr,targetMap);
            clientChannel.writeAndFlush("您已选择和用户【" + targetAddr + "】私聊！\n");
            return;
        } else if (msg.toString().contains("To:全部")) {
            Map<String,Integer> targetMap = clientMap.get(currentAddr);
            // 进入群聊将私聊全部标识为0
            if (!targetMap.isEmpty()) {
                Set<String> keys = targetMap.keySet();
                keys.forEach(key->{
                    targetMap.put(key,0);
                });
            }
            channels.forEach(channel -> {
                if (channel != clientChannel) {
                    // 返回结构
                /*RpcResponse response = new RpcResponse();
                response.setId(UUID.randomUUID().toString());
                response.setStatus(1);*/
                    //response.setData("【提示】：用户【" + clientChannel.remoteAddress() + "】加入聊天室！");
                    //channel.writeAndFlush("【提示】：用户【" + clientChannel.remoteAddress() + "】加入聊天室！");
                    channel.writeAndFlush("【提示】：用户【" + clientChannel.remoteAddress() + "】加入聊天室！");
                } else {
                    channel.writeAndFlush("【提示】：您已经加入聊天室！");
                }
            });
            return;
        }
        // 判断当前用户是否处于私聊
        Map<String,Integer> targetMap = clientMap.get(currentAddr);
        if (Objects.nonNull(targetMap) && !targetMap.isEmpty()) {
            Set<String> keys = targetMap.keySet();
            // 查看是否存在标识为1的私聊用户
            Optional<String> keyOptional = keys.stream().filter(key->targetMap.get(key) == 1).findFirst();
            List<Boolean> list = new ArrayList<>();
            keyOptional.ifPresent(s -> {
                // 存在时获取私聊用户通道
                Optional<Channel> channelOptional = channels.stream().filter(channel -> channel.remoteAddress().toString().contains(s)).findFirst();
                channelOptional.ifPresent(targetChannel -> {
                    channels.forEach(channel -> {
                        if (channel == targetChannel) {
                            // 返回结构
                            /*RpcResponse response = new RpcResponse();
                            response.setId(UUID.randomUUID().toString());
                            response.setStatus(1);*/
                            //response.setData("【提示】：用户【" + clientChannel.remoteAddress() + "】加入聊天室！");
                            //channel.writeAndFlush("【提示】：用户【" + clientChannel.remoteAddress() + "】加入聊天室！");
                            targetChannel.writeAndFlush("用户【" + clientChannel.remoteAddress() + "】对你说：" + msg.toString() + "\n");
                            list.add(true);
                        } else if (channel == clientChannel) {
                            clientChannel.writeAndFlush("你对用户【" + targetChannel.remoteAddress() + "】说：" + msg.toString() + "\n");
                            list.add(true);
                        }
                    });
                });
            });
            if (!list.isEmpty()) {
                return;
            }
        }
        // 不存在私聊时进行群聊转发
        channels.forEach(channel -> {
            // 返回结构
            /*RpcResponse response = new RpcResponse();
            response.setId(UUID.randomUUID().toString());
            response.setStatus(1);*/
            if (!channel.equals(clientChannel)) {
                //response.setData("用户【" + clientChannel.remoteAddress() + "】说：" + rpcRequest.toString() + "\n");
                //channel.writeAndFlush("用户【" + clientChannel.remoteAddress() + "】说：" + rpcRequest.toString() + "\n");
                channel.writeAndFlush("用户【" + clientChannel.remoteAddress() + "】说：" + msg.toString() + "\n");
            } else {
                //response.setData("【我】说：" + rpcRequest.toString() + "\n");
                //channel.writeAndFlush("【我】说：" + rpcRequest.toString() + "\n");
                channel.writeAndFlush("【我】说：" + msg.toString() + "\n");
            }
        });
        // System.out.println("接收到客户端信息:" + rpcRequest.toString());
    }

    // 通知处理器最后的channelRead()是当前批处理中的最后一条消息时调用

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        System.out.println("服务端接收数据完毕..");
        ctx.flush();
    }

    // 读操作时捕获到异常时调用

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }

    // 客户端去和服务端连接成功时触发

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel clientChannel = ctx.channel();
        System.out.println("用户【"+clientChannel.remoteAddress()+"】在线中...");
        channels.forEach(channel -> {
            if (channel != clientChannel) {
                // 返回结构
                /*RpcResponse response = new RpcResponse();
                response.setId(UUID.randomUUID().toString());
                response.setStatus(1);*/
                //response.setData("【提示】：用户【" + clientChannel.remoteAddress() + "】加入聊天室！");
                //channel.writeAndFlush("【提示】：用户【" + clientChannel.remoteAddress() + "】加入聊天室！");
                channel.writeAndFlush("add:" + splitAddress(clientChannel.remoteAddress().toString()));

            } else {
                channel.writeAndFlush("add:" + getOtherClient(clientChannel));
            }
        });
        //ctx.writeAndFlush("hello client");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel clientChannel = ctx.channel();
        System.out.println("用户【"+clientChannel.remoteAddress()+"】离线了");
        channels.forEach(channel -> {
            if (channel != clientChannel) {
                // 返回结构
                /*RpcResponse response = new RpcResponse();
                response.setId(UUID.randomUUID().toString());
                response.setStatus(1);*/
                //response.setData("【提示】：用户【" + clientChannel.remoteAddress() + "】加入聊天室！");
                //channel.writeAndFlush("【提示】：用户【" + clientChannel.remoteAddress() + "】加入聊天室！");
                channel.writeAndFlush("remove:" + splitAddress(clientChannel.remoteAddress().toString()));

            }
        });
    }

    public static String splitAddress(String address){
        // 获取用户端口
        String[] result = address.split(":");
        return result[result.length-1];
    }

    public static String getClientTables(Channel clientChannel){
        // 针对请求用户返回用户信息
        StringBuffer sb = new StringBuffer();
        channels.forEach(channel -> {
            if (channel != clientChannel) {
                sb.append(splitAddress(channel.remoteAddress().toString()) + "\n");
            }
        });
        return sb.toString();
    }

    public static String getOtherClient(Channel clientChannel){
        // 针对请求用户返回用户信息
        StringBuffer sb = new StringBuffer();
        channels.forEach(channel -> {
            if (channel != clientChannel) {
                sb.append(splitAddress(channel.remoteAddress().toString()) + ":");
            }
        });
        return sb.toString();
    }
}
