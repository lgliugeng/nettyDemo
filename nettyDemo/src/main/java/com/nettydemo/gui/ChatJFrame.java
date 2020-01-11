package com.nettydemo.gui;

import com.nettydemo.netty.NettyClient;
import io.netty.channel.Channel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

public class ChatJFrame  extends JFrame {
    private static final long serialVersionUID = 1L;
    private JPanel panelTool;
    private JButton btnSend;
    private JTextField txtInput;
    public static JTextArea txtaShow;
    //下拉菜单
    public static JComboBox<String> cmbUsers;
    //滚动条
    private JScrollPane srlp;

    public ChatJFrame(){
        super("聊天窗口");
        txtaShow = new JTextArea();
        srlp = new JScrollPane(txtaShow);
        this.add(srlp,BorderLayout.CENTER);
        panelTool = new JPanel();
        String [] names = new String[]{"全部"};
        cmbUsers = new JComboBox<String>(names);
        panelTool.add(cmbUsers);
        txtInput = new JTextField(10);
        panelTool.add(txtInput);
        btnSend = new JButton("发送");
        panelTool.add(btnSend);
        this.add(panelTool,BorderLayout.SOUTH);
        btnSend.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String msg = txtInput.getText();
                System.out.println(msg);
                //消息体
                if (channel == null) {
                    txtaShow.append("【提示】:服务器未启动！！！\r\n");
                    return;
                }
                channel.writeAndFlush(msg);
                /*txtaShow.append("我对" + cmbUsers.getSelectedItem()+"说:"+
                        msg+"\r\n");*/
            }
        });
        cmbUsers.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    System.out.println("当前选择："+e.getItem());
                    channel.writeAndFlush("To:" + e.getItem());
                }

            }
        });
        this.setVisible(true);
        setLocation(350, 150);
        setBackground(Color.CYAN);
        setSize(300, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private static Channel channel = null;
    public static void main(String[] args) {
        ChatJFrame cf = new ChatJFrame();
        try {
            NettyClient client = new NettyClient("192.168.13.68",8888);
            //启动client服务
            client.start();

            channel = client.getChannel();
        }catch (Exception e){

        }
    }
}
