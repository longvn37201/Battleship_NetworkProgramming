package server;

import java.io.IOException;
import java.net.ServerSocket;

import static util.Configs.PORT_NUMBER;

public class MainServer {

    public MainServer(int portNumber) {
        try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
            System.out.println("SERVER ĐANG CHẠY Ở CỔNG: " + PORT_NUMBER);
            while (true) {
                new PlayerHandler(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new MainServer(PORT_NUMBER);
    }
}
