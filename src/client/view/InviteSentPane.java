package client.view;

import client.Client;

import javax.swing.*;
import java.awt.*;

public class InviteSentPane extends JOptionPane {

    private JDialog dialog;
    private Client client;

    public InviteSentPane(String name, Client client) {
        super();
        this.setMessage("Chờ " + name + " phản hồi.");
        this.setMessageType(CANCEL_OPTION);
        String[] options = {"Hủy"};
        this.setOptions(options);
        this.client = client;
    }

    public void showPane(Component parent) {
        dialog = this.createDialog(parent, "Đã gửi lời mời");
        dialog.setVisible(true);
        dialog.dispose();
        if (getValue() == "Hủy") {
            client.sendStringArray(new String[]{"join", "cancel"});
        }
    }

    public void dispose() {
        dialog.dispose();
    }
}
