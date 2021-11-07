package client;

import client.view.GameView;
import client.view.InviteReceivedPane;
import client.view.InviteSentPane;
import client.view.WaitingRoomView;
import model.messages.MatchRoomListMessage;
import model.messages.NotificationMessage;
import util.Constants;

import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.HashMap;

import static util.Constants.Configs.HOST_NAME;
import static util.Constants.Configs.PORT_NUMBER;
import static util.Constants.NotificationCode.*;

public class Client extends Thread {

    private WaitingRoomView waitingRoomView;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private volatile GameHandler gameHandlerModel;
    private String key = "";
    private String ownName;
    private volatile NameState nameState;
    private HashMap<String, InviteReceivedPane> inviteDialogs;
    private InviteSentPane inviteSentPane;

    public Client(WaitingRoomView waitingRoomView) {
        this.waitingRoomView = waitingRoomView;

        boolean connected = false;

        while (!connected) {
            try {
                Socket socket = new Socket(HOST_NAME, PORT_NUMBER);
                out = new ObjectOutputStream((socket.getOutputStream()));
                in = new ObjectInputStream(socket.getInputStream());
                out.flush();

                System.out.println("remote server socket: " + socket.getRemoteSocketAddress().toString());
                connected = true;
            } catch (IOException e) {
                int response = waitingRoomView.showInitialConnectionError();
                if (response == 0) {
                    System.exit(-1);
                }
            }
        }

        inviteDialogs = new HashMap<>();

        start();
    }

    @Override
    public void run() {
        super.run();
        Object input;
        try {
            while ((input = in.readObject()) != null) {
                if (gameHandlerModel != null) {
                    //xử lí giao tiếp trong ván game
                    gameHandlerModel.parseInput(input);
                } else {
                    //xử lí giao tiếp ngoài sảnh chờ
                    parseInput(input);
                }
            }
            System.out.println("stopped");
        } catch (IOException e) {
            waitingRoomView.showLostConnectionError();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void sendJoinGameRequest(String key, final String name) {
        try {
            System.out.println(">> " + NEW_JOIN_GAME_REQUEST + " " + key);
            out.writeObject(new String[]{"join", "join", key});
            out.flush();
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    inviteSentPane = new InviteSentPane(name, Client.this);
                    inviteSentPane.showPane(waitingRoomView);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendName(String name) {
        this.nameState = NameState.WAITING;
        System.out.println(">> " + Constants.NotificationCode.NAME_REQUEST + " " + name);
        sendStringArray(new String[]{"name", name});
    }

    // gui request den server de join sảnh
    public void joinLobby() {
        sendStringArray(new String[]{"join", "start"});
    }

    public static enum NameState {
        WAITING, ACCEPTED, INVALID, TAKEN
    }

    private void setNameState(NameState nameState) {
        synchronized (this) {
            this.nameState = nameState;
            this.notifyAll();
        }
    }

    public NameState getNameState() {
        return nameState;
    }

    private void parseInput(Object input) {

        if (input instanceof MatchRoomListMessage) {
            final HashMap<String, String> matchRoomList = ((MatchRoomListMessage) input).getMatchRoomList();
            waitingRoomView.updateWaitingList(matchRoomList);
        } else if (input instanceof NotificationMessage) {
            NotificationMessage n = (NotificationMessage) input;

            if (n.getCode() != Constants.NotificationCode.OPPONENTS_NAME) {
                System.out.print("<< " + n.getCode());
            }

            switch (n.getCode()) {
                case GAME_TOKEN:
                    if (n.getText().length == 1) {
                        key = n.getText()[0];
                        System.out.println(" " + key);
                    }
                    break;
                case OPPONENTS_NAME:
                    disposeAllPanes();
                    startGame(input);
                    break;
                case NAME_ACCEPTED:
                    setNameState(NameState.ACCEPTED);
                    System.out.println("");
                    break;
                case NAME_TAKEN:
                    setNameState(NameState.TAKEN);
                    System.out.println("");
                    break;
                case INVALID_NAME:
                    setNameState(NameState.INVALID);
                    System.out.println("");
                    break;
                case NEW_JOIN_GAME_REQUEST:
                    final InviteReceivedPane dialog = new InviteReceivedPane(n.getText()[0], n.getText()[1], this);
                    System.out.println(" " + n.getText()[0]);
                    inviteDialogs.put(n.getText()[0], dialog);
                    EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            dialog.showOptionPane(waitingRoomView);
                        }
                    });
                    break;
                case JOIN_GAME_REQUEST_REJECTED:
                    if (inviteSentPane != null) {
                        inviteSentPane.dispose();
                    }
                    System.out.println("");
                    break;
                case JOIN_GAME_REQUEST_ACCEPTED:
                    System.out.println("");
                    break;
                case JOIN_GAME_REQUEST_CANCELLED:
                    InviteReceivedPane pane = inviteDialogs.get(n.getText()[0]);
                    if (pane != null) {
                        pane.dispose();
                    } else {
                        System.out.println("can't find " + n.getText()[0]);
                    }
            }
        }
    }

    private void startGame(Object firstInput) {
        waitingRoomView.setVisible(false);
        GameView gameView = new GameView(this.out, this.in, this);
        gameHandlerModel = gameView.getModel();
        gameHandlerModel.parseInput(firstInput);
    }

    public String getKey() {
        return key;
    }

    public void reopen() {
        if (gameHandlerModel != null) {
            this.gameHandlerModel.getView().dispose();
            this.gameHandlerModel = null;
        }
        waitingRoomView.setVisible(true);
        joinLobby();
    }

    public void sendStringArray(String[] array) {
        try {
            out.writeObject(array);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void disposeAllPanes() {
        for (InviteReceivedPane pane : inviteDialogs.values()) {
            pane.dispose();
        }
        if (inviteSentPane != null) {
            inviteSentPane.dispose();
        }
    }

    public void setOwnName(String ownName) {
        this.ownName = ownName;
    }

    public String getOwnName() {
        return ownName;
    }
}
