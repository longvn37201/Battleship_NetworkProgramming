package server;

import model.game.Board;
import model.messages.ChatMessage;
import model.messages.MoveMessage;
import model.messages.NotificationMessage;
import util.NotificationCode;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;


public class Player extends Thread {
    public Socket socket;
    private MatchRoom matchRoom;
    private String name = "";
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Game game;
    private Board board;
    private HashMap<String, Player> requestList;
    private String ownKey;
    private String requestedGameKey;

    public Player(Socket socket, MatchRoom matchRoom) {
        this.socket = socket;
        this.matchRoom = matchRoom;
        matchRoom.assignKey(this);
        matchRoom.addPlayer(this);
        this.requestList = new HashMap<>();
        System.out.println(">> " + socket.getRemoteSocketAddress().toString() + " " + NotificationCode.PLAYER_CONNECTED + " " + "connected");
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            Object input;
            while ((input = in.readObject()) != null) {
                //type string[]
                if (input instanceof String[]) {
                    String[] array = (String[]) input;
                    int length = array.length;

                    if (length > 0) {
                        String message = array[0];

                        switch (message) {
                            case "join":
                                matchRoom.parse(this, array);
                                break;
                            case "name":
                                System.out.println("<< " + socket.getRemoteSocketAddress().toString() + " " + NotificationCode.NAME_REQUEST + " " + array[1]);
                                if (length != 2 || array[1] == null || array[1].equals("")) {
                                    System.out.println(">> " + socket.getRemoteSocketAddress().toString() + " " + NotificationCode.INVALID_NAME + " ");
                                    writeNotification(NotificationCode.INVALID_NAME);
                                } else if (matchRoom.playerNameExists(array[1])) {
                                    System.out.println(">> " + socket.getRemoteSocketAddress().toString() + " " + NotificationCode.NAME_TAKEN + " " + array[1]);
                                    writeNotification(NotificationCode.NAME_TAKEN);
                                } else {
                                    name = array[1];
                                    System.out.println(">> " + socket.getRemoteSocketAddress().toString() + " " + NotificationCode.NAME_ACCEPTED + " " + name);
                                    writeNotification(NotificationCode.NAME_ACCEPTED);
                                    matchRoom.sendMatchRoomList();
                                }
                                break;
                        }
                    }
                } else if (input instanceof Board) {
                    Board board = (Board) input;

                    // Print Board nhân được từ Client
                    System.out.println("<< " + socket.getRemoteSocketAddress().toString() + " " + NotificationCode.SEND_BOARD);
                    board.printBoard(true);

                    if (Board.isValid(board) && game != null) {
                        System.out.println(">> " + socket.getRemoteSocketAddress().toString() + " " + NotificationCode.BOARD_ACCEPTED);
                        writeNotification(NotificationCode.BOARD_ACCEPTED);
                        this.board = board;
                        //neu 2 player board hợp lệ thì start game
                        game.checkBoards();
                    } else if (game == null) {
                        System.out.println(">> " + socket.getRemoteSocketAddress().toString() + " " + NotificationCode.NOT_IN_GAME);
                        writeNotification(NotificationCode.NOT_IN_GAME);
                    } else {
                        System.out.println(">> " + socket.getRemoteSocketAddress().toString() + " " + NotificationCode.INVALID_BOARD);
                        writeNotification(NotificationCode.INVALID_BOARD);
                    }
                } else if (input instanceof MoveMessage) {
                    if (game != null) {
                        game.applyMove((MoveMessage) input, this);
                    }
                } else if (input instanceof ChatMessage) {
                    if (game != null) {
                        Player opponent = game.getOpponent(this);
                        if (opponent != null) {
                            opponent.writeObject(input);
                        }
                    }
                }
            }

        } catch (IOException e) {
            if (game != null) {
                leaveGame();
            } else {
                matchRoom.removeWaitingPlayer(this);
            }
            matchRoom.removePlayer(this);
            System.out.println(">> " + socket.getRemoteSocketAddress().toString() + " connected");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public String getPlayerName() {
        return name;
    }

    //gui mess đến 1 client
    public void writeMessage(String message) {
        try {
            out.writeObject(message);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //gui obj den client
    public void writeObject(Object object) {
        try {
            out.writeObject(object);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //gui thong bao den client co the kem them Thong tin bo sung
    public void writeNotification(int notificationMessage, String... text) {
        try {
            NotificationMessage nm = new NotificationMessage(notificationMessage, text);
            out.writeObject(nm);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Board getBoard() {
        return this.board;
    }

    // Send  game request đến player, và update req list, req game key
    //  requester là player gửi request
    public synchronized void sendRequest(Player requester) {
        requestList.put(requester.getOwnKey(), requester);
        requester.requestedGameKey = this.ownKey;
        System.out.println(">> " + socket.getRemoteSocketAddress().toString() + " " + NotificationCode.NEW_JOIN_GAME_REQUEST + " " + requester.ownKey);
        writeNotification(NotificationCode.NEW_JOIN_GAME_REQUEST, requester.getOwnKey(), requester.getPlayerName());
    }

    //  đối thủ chấp nhận một yêu cầu và thông báo cho người chơi
    //  opponent là player  đã accept request
    public synchronized void requestAccepted(Player opponent) {
        opponent.requestList.remove(ownKey);
        requestedGameKey = null;
        System.out.println(">> " + socket.getRemoteSocketAddress().toString() + " " + NotificationCode.JOIN_GAME_REQUEST_ACCEPTED + " " + this.ownKey);
        writeNotification(NotificationCode.JOIN_GAME_REQUEST_ACCEPTED);
    }

    // đối thủ từ chối một yêu cầu
    public synchronized void requestRejected(Player opponent) {
        opponent.requestList.remove(ownKey);
        requestedGameKey = null;
        System.out.println(">> " + socket.getRemoteSocketAddress().toString() + " " + NotificationCode.JOIN_GAME_REQUEST_REJECTED + " " + this.ownKey);
        writeNotification(NotificationCode.JOIN_GAME_REQUEST_REJECTED);
    }

    //set own key cho player
    public void setOwnKey(String ownKey) {
        this.ownKey = ownKey;
    }

    public String getOwnKey() {
        return ownKey;
    }

    public void setRequestedGameKey(String key) {
        this.requestedGameKey = key;
    }

    // key của player gửi req
    public String getRequestedGameKey() {
        return requestedGameKey;
    }

    //Rejects all game invite
    public void rejectAll() {
        for (Player p : requestList.values()) {
            p.requestRejected(this);
        }
    }

    //End game và thông báo cho đối thủ
    public void leaveGame() {
        if (game != null) {
            Player opponent = game.getOpponent(this);
            System.out.println(">> " + socket.getRemoteSocketAddress().toString() + " " + NotificationCode.OPPONENT_DISCONNECTED + " ");
            opponent.writeNotification(NotificationCode.OPPONENT_DISCONNECTED);
            game.killGame();
        }
    }
}
