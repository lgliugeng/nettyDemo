package com.nettydemo.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class NioChatClient2 {

    public static void main(String[] args) {
        String username = "Honj";
        NioClientThread client = new NioClientThread(username);
        client.start();
        // 输入\输出流
        BufferedReader sin = new BufferedReader(new InputStreamReader(System.in));
        try {
            String readLine;
            while ((readLine = sin.readLine().trim()) != null) {
                if(readLine.equals("byte")) {
                    client.close();
                    System.exit(0);
                }
                client.send(username + ":" + readLine);
            }
        }catch (IOException e){

        }
    }
}
