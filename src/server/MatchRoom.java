package server;

import model.messages.MatchRoomListMessage;
import util.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MatchRoom {

    private HashMap<String, Player> waitingPlayerList;
    private ArrayList<Player> connectedPlayers;

    public MatchRoom() {
        this.waitingPlayerList = new HashMap<String, Player>();
        this.connectedPlayers = new ArrayList<>();
    }

    //  đưa key vào HashMap, gửi lại cho người chơi
    //  để người chơi join vào danh sách phòng chờ
    public synchronized void joinWaitingList(Player player) {
        waitingPlayerList.put(player.getOwnKey(), player);
        player.writeNotification(Constants.NotificationCode.GAME_TOKEN, player.getOwnKey());
        sendMatchRoomList();
    }

    //gửi request từ player đến player khác thông qua key
    public synchronized void joinRequest(Player player, String key) {
        Player opponent = waitingPlayerList.get(key);
        if (player == opponent) {
            player.writeNotification(Constants.NotificationCode.CANNOT_PLAY_YOURSELF);
        } else if (opponent != null) {
            opponent.sendRequest(player);
        }
    }

    //player acp request từ player có key được cung cấp
    public synchronized void acceptRequest(Player player, String key) {
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
    public synchronized void rejectRequest(Player player, String key) {
        Player opponent = waitingPlayerList.get(key);
        if (opponent != null &&
                opponent.getRequestedGameKey().equals(player.getOwnKey())) {
            opponent.requestRejected(player);
        }
    }

    // request từ player bị cancel
    public synchronized void cancelRequest(Player player) {
        Player opponent = waitingPlayerList.get(player.getRequestedGameKey());
        player.setRequestedGameKey(null);
        if (opponent != null) {
            opponent.writeNotification(
                    Constants.NotificationCode.JOIN_GAME_REQUEST_CANCELLED,
                    player.getOwnKey());
            System.out.println (">> " + opponent.socket.getRemoteSocketAddress().toString() + " " + Constants.NotificationCode.JOIN_GAME_REQUEST_CANCELLED );
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
