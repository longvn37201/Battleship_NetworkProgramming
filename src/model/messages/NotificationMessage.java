package model.messages;

import java.io.Serializable;

//notification tu server gui client
public class NotificationMessage implements Serializable {


    private int code;
    private String[] text;

    public NotificationMessage(int code) {
        this.code = code;
    }

    public NotificationMessage(int code, String... text) {
        this.code = code;
        this.text = text;
    }

    public int getCode() {
        return code;
    }

    public String[] getText() {
        return text;
    }
}
