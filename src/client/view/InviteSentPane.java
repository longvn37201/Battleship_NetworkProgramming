package client.view;

import client.MatchRoom;

import javax.swing.*;
import java.awt.*;

public class InviteSentPane extends JOptionPane {

    private JDialog dialog;
    private MatchRoom matchRoom;

    public InviteSentPane(String name, MatchRoom matchRoom) {
        super();
        this.setMessage("Chờ " + name + " phản hồi.");
        this.setMessageType(CANCEL_OPTION);
        String[] options = {"Hủy"};
        this.setOptions(options);
        this.matchRoom = matchRoom;
    }

    public void showPane(Component parent) {
        dialog = this.createDialog(parent, "Đã gửi lời mời");
        dialog.setVisible(true);
        dialog.dispose();
        if (getValue() == "Hủy") {
            matchRoom.sendStringArray(new String[]{"join", "cancel"});
        }
    }

    public void dispose() {
        dialog.dispose();
    }
}
