package model.messages;

import java.io.Serializable;
import java.util.HashMap;

// Object gửi client chứa danh sách các người chơi đang phòng chờ
public class MatchRoomListMessage implements Serializable {

    private HashMap<String, String> matchRoomList;

    public MatchRoomListMessage(HashMap<String, String> matchRoomList) {
        this.matchRoomList = matchRoomList;
    }

    public HashMap<String, String> getMatchRoomList() {
        return this.matchRoomList;
    }

}
