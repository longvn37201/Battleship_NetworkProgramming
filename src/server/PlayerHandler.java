package server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class PlayerHandler extends Thread {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String name = "";
    //    private Game game;
//    private Board board;
    private String ownKey;

    public PlayerHandler(Socket socket) {
        System.out.println("-" + socket.getRemoteSocketAddress());
        this.socket = socket;

    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            Object input;
            while ((input = in.readObject()) != null) {
                //todo
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
