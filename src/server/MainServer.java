package server;

import java.io.IOException;
import java.net.ServerSocket;

import static util.Configs.PORT_NUMBER;

public class MainServer {

    public MainServer(int portNumber) {
        try {
            ServerSocket serverSocket = new ServerSocket(portNumber);
            System.out.println("SERVER ĐANG CHẠY Ở CỔNG: " + portNumber);

            MatchRoom matchRoom = new MatchRoom();
            while (true) {
                new Player(serverSocket.accept(), matchRoom).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        new MainServer(PORT_NUMBER);
    }

}
