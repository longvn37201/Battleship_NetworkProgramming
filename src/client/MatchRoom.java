package client;

import client.view.ClientView;
import client.view.InviteReceivedPane;
import client.view.InviteSentPane;
import client.view.MatchRoomView;
import model.messages.MatchRoomListMessage;
import model.messages.NotificationMessage;
import util.NotificationCode;

import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.HashMap;

import static util.Configs.HOST_NAME;
import static util.Configs.PORT_NUMBER;
import static util.NotificationCode.NEW_JOIN_GAME_REQUEST;

public class MatchRoom extends Thread {

    private MatchRoomView matchRoomView;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private volatile Client clientModel;
    private String key = "";
    private String ownName;
    private volatile NameState nameState;
    private HashMap<String, InviteReceivedPane> inviteDialogs;
    private InviteSentPane inviteSentPane;

    public String serverAddress;

    public MatchRoom(MatchRoomView matchRoomView) {
        this.matchRoomView = matchRoomView;

        boolean connected = false;

        while (!connected) {
            try {
                String hostname = HOST_NAME;
                int port = PORT_NUMBER;
                Socket socket = new Socket(hostname, port);
                out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                in = new ObjectInputStream(socket.getInputStream());
                out.flush();

                serverAddress = socket.getRemoteSocketAddress().toString();
                System.out.println(serverAddress);
                connected = true;
            } catch (IOException e) {
                int response = matchRoomView.showInitialConnectionError();
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
                // System.out.println(input);
                if (clientModel != null) {
                    clientModel.parseInput(input);
                } else {
                    parseInput(input);
                }
            }
            System.out.println("stopped");
        } catch (IOException e) {
            matchRoomView.showLostConnectionError();
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
                    inviteSentPane = new InviteSentPane(name, MatchRoom.this);
                    inviteSentPane.showPane(matchRoomView);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendName(String name) {
        this.nameState = NameState.WAITING;
        System.out.println(">> " + NotificationCode.NAME_REQUEST + " " + name);
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
//            EventQueue.invokeLater(new Runnable() {
//                @Override
//                public void run() {
//                    matchRoomView.updateMatchRoomList(matchRoomList);
//                }
//            });
            matchRoomView.updateMatchRoomList(matchRoomList);
        } else if (input instanceof NotificationMessage) {
            NotificationMessage n = (NotificationMessage) input;

            if (n.getCode() != NotificationCode.OPPONENTS_NAME) {
                System.out.print("<< " + n.getCode());
            }

            switch (n.getCode()) {
                case NotificationCode.GAME_TOKEN:
                    if (n.getText().length == 1) {
                        key = n.getText()[0];
                        System.out.println(" " + key);
                    }
                    break;
                case NotificationCode.OPPONENTS_NAME:
                    disposeAllPanes();
                    startGame(input);
                    break;
                case NotificationCode.NAME_ACCEPTED:
                    setNameState(NameState.ACCEPTED);
                    System.out.println("");
                    break;
                case NotificationCode.NAME_TAKEN:
                    setNameState(NameState.TAKEN);
                    System.out.println("");
                    break;
                case NotificationCode.INVALID_NAME:
                    setNameState(NameState.INVALID);
                    System.out.println("");
                    break;
                case NotificationCode.NEW_JOIN_GAME_REQUEST:
                    final InviteReceivedPane dialog = new InviteReceivedPane(n.getText()[0], n.getText()[1], this);
                    System.out.println(" " + n.getText()[0]);
                    inviteDialogs.put(n.getText()[0], dialog);
                    EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            dialog.showOptionPane(matchRoomView);
                        }
                    });
                    break;
                case NotificationCode.JOIN_GAME_REQUEST_REJECTED:
                    if (inviteSentPane != null) {
                        inviteSentPane.dispose();
                    }
                    System.out.println("");
                    break;
                case NotificationCode.JOIN_GAME_REQUEST_ACCEPTED:
                    System.out.println("");
                    break;
                case NotificationCode.JOIN_GAME_REQUEST_CANCELLED:
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
        matchRoomView.setVisible(false);
        ClientView clientView = new ClientView(this.out, this.in, this);
        clientModel = clientView.getModel();
        clientModel.parseInput(firstInput);
    }

    public String getKey() {
        return key;
    }

    public void reopen() {
        if (clientModel != null) {
            this.clientModel.getView().dispose();
            this.clientModel = null;
        }
        matchRoomView.setVisible(true);
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
