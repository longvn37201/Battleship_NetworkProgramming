package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;

import static util.Constants.Configs.PORT_NUMBER;

public class Server {
    private HashMap<String, Player> waitingPlayerList = new HashMap<String, Player>();
    private ArrayList<Player> connectedPlayers = new ArrayList<>();

    public Server(int portNumber) {
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

    public static void main(String[] args) {
        new Server(PORT_NUMBER);
    }
}
