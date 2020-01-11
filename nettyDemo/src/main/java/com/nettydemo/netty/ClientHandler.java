package com.nettydemo.netty;

import com.nettydemo.gui.ChatJFrame;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.springframework.util.StringUtils;

public class ClientHandler extends SimpleChannelInboundHandler<String> {

    //处理服务端返回的数据

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        System.out.println("接受到server响应数据: " + msg);
        if (!StringUtils.isEmpty(msg) && msg.contains("add:")) {
            String[] names = msg.split(":");
            System.out.println(1);
            if (ChatJFrame.cmbUsers != null) {
                for (int i = 1; i < names.length; i++) {
                    if (!StringUtils.isEmpty(names[i])) {
                        ChatJFrame.cmbUsers.addItem(names[i]);
                    }
                }
            }
        } else if (!StringUtils.isEmpty(msg) && msg.contains("remove:")) {
            String[] names = msg.split(":");
            if (ChatJFrame.cmbUsers != null) {
                ChatJFrame.cmbUsers.removeItem(names[names.length-1]);
            }
        } else {
            System.out.println(2);
            if (ChatJFrame.txtaShow != null) {
                ChatJFrame.txtaShow.append(msg + "\r\n");
            }
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }
}
