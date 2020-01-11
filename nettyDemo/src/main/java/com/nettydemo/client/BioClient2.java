package com.nettydemo.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Scanner;

public class BioClient2 {

    public static void main(String[] args) {
        try {
            Socket socket = new Socket("192.168.13.68",8888);
            while (true){
                Scanner scanner = new Scanner(System.in);
                socket.getOutputStream().write(scanner.next().getBytes("utf-8"));
                socket.getOutputStream().flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
