package model.messages;

import java.io.Serializable;

// object gửi từ player chứa tọa độ người chơi đi
public class MoveMessage implements Serializable {

    private int x;
    private int y;

    public MoveMessage(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }
}
