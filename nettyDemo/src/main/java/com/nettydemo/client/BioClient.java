package com.nettydemo.client;

import java.io.IOException;
import java.net.Socket;

public class BioClient {

    public static void main(String[] args) {
        try {
            Socket socket = new Socket("192.168.13.68",8888);
            socket.getOutputStream().write("hello".getBytes());
            socket.getOutputStream().flush();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
