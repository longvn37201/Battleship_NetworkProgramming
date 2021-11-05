package server;

import model.messages.MatchRoomListMessage;
import util.NotificationCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class MatchRoom {

    private final String ALPHABET = "abcdefghijklmnopqrstuvwxyz";
    private HashMap<String, Player> waitingPlayerList;
    private ArrayList<Player> connectedPlayers;

    public MatchRoom() {
        this.waitingPlayerList = new HashMap<String, Player>();
        this.connectedPlayers = new ArrayList<>();
    }


    //  chuyển đổi messages từ client.
    public void parse(Player player, String[] args) {
        if (args.length < 2 || player.getPlayerName().equals("")) {
            return;
        }
        String option = args[1];
        switch (option) {
            case "start":
                player.leaveGame();
                joinWaitingList(player);
                break;
            case "join":
                player.leaveGame();
                if (args.length == 3) {
                    player.leaveGame();
                    System.out.println ("<< " + player.socket.getRemoteSocketAddress().toString() + " " + NotificationCode.NEW_JOIN_GAME_REQUEST  + " " + args[2]);
                    joinRequest(player, args[2]);
                }
                break;
            case "accept":
                player.leaveGame();
                if (args.length == 3) {
                    System.out.println ("<< " + player.socket.getRemoteSocketAddress().toString() + " " + NotificationCode.JOIN_GAME_REQUEST_ACCEPTED + " " + args[2]);
                    acceptRequest(player, args[2]);
                }
                break;
            case "reject":
                if (args.length == 3) {
                    System.out.println ("<< " + player.socket.getRemoteSocketAddress().toString() + " " + NotificationCode.JOIN_GAME_REQUEST_REJECTED + " " + args[2]);
                    rejectRequest(player, args[2]);
                }
            case "cancel":
                if (args.length == 2) {
                    System.out.println ("<< " + player.socket.getRemoteSocketAddress().toString() + " " + NotificationCode.JOIN_GAME_REQUEST_CANCELLED );
                    cancelRequest(player);
                }
        }
    }

    //  đưa key vào HashMap, gửi lại cho người chơi
    //  để người chơi join vào danh sách phòng chờ
    private synchronized void joinWaitingList(Player player) {
        waitingPlayerList.put(player.getOwnKey(), player);
        player.writeNotification(NotificationCode.GAME_TOKEN, player.getOwnKey());
        sendMatchRoomList();
    }

    //sinh key cho người chơi
    public synchronized void assignKey(Player player) {
        StringBuilder keyBuilder = new StringBuilder();
        Random random = new Random();
        int length = ALPHABET.length();
        for (int i = 0; i < 10; ++i) {
            keyBuilder.append(ALPHABET.charAt(random.nextInt(length)));
        }
        String key = keyBuilder.toString();
        player.setOwnKey(key);
    }

    //gửi request từ player đến player khác thông qua key
    private synchronized void joinRequest(Player player, String key) {
        Player opponent = waitingPlayerList.get(key);
        if (player == opponent) {
            player.writeNotification(NotificationCode.CANNOT_PLAY_YOURSELF);
        } else if (opponent != null) {
            opponent.sendRequest(player);
        }
    }

    //player acp request từ player có key được cung cấp
    private synchronized void acceptRequest(Player player, String key) {
        Player opponent = waitingPlayerList.get(key);
        if (opponent != null && opponent.getRequestedGameKey().equals(player.getOwnKey())) {
            waitingPlayerList.remove(key);
            waitingPlayerList.values().remove(player);
            opponent.requestAccepted(player);
            new Game(opponent, player);
            sendMatchRoomList();
            player.rejectAll();
            opponent.rejectAll();
        }
    }

    //player reject request từ player có key được cung cấp
    private synchronized void rejectRequest(Player player, String key) {
        Player opponent = waitingPlayerList.get(key);
        if (opponent != null &&
                opponent.getRequestedGameKey().equals(player.getOwnKey())) {
            opponent.requestRejected(player);
        }
    }

    // request từ player bị cancel
    private synchronized void cancelRequest(Player player) {
        Player opponent = waitingPlayerList.get(player.getRequestedGameKey());
        player.setRequestedGameKey(null);
        if (opponent != null) {
            opponent.writeNotification(
                    NotificationCode.JOIN_GAME_REQUEST_CANCELLED,
                    player.getOwnKey());
            System.out.println (">> " + opponent.socket.getRemoteSocketAddress().toString() + " " + NotificationCode.JOIN_GAME_REQUEST_CANCELLED );
        }
    }

    //remove player trong waitting room
    //Gửi lại danh sách chờ cho các player còn lại
    public synchronized void removeWaitingPlayer(Player player) {
        waitingPlayerList.values().remove(player);
        sendMatchRoomList();
    }

    //check player name đã tồn tại
    public boolean playerNameExists(String name) {
        for (Player player : connectedPlayers) {
            if (name.equals(player.getPlayerName())) {
                return true;
            }
        }
        return false;
    }

    //gui danh sach cách player khác trong sảnh chờ cho tất cả người chơi
    public synchronized void sendMatchRoomList() {
        HashMap<String, String> matchRoomList = new HashMap<String, String>();
        for (Map.Entry<String, Player> entry : waitingPlayerList.entrySet()) {
            String key = entry.getKey();
            Player player = entry.getValue();
            matchRoomList.put(key, player.getPlayerName());
        }
        MatchRoomListMessage message = new MatchRoomListMessage(matchRoomList);
        for (Map.Entry<String, Player> entry : waitingPlayerList.entrySet()) {
            Player player = entry.getValue();
            player.writeObject(message);
        }
    }

    //thêm player vào danh sách đã kết nối phong chờ
    public void addPlayer(Player player) {
        if (!connectedPlayers.contains(player)) {
            connectedPlayers.add(player);
        }
    }

    //xóa player khỏi danh sách đã kết nối phong chờ
    public void removePlayer(Player player) {
        connectedPlayers.remove(player);
    }

}
