package client.view;

import client.Client;

import javax.swing.*;
import java.awt.*;

public class InviteReceivedPane extends JOptionPane {

    private JDialog dialog;
    private Client client;
    private String key;

    public InviteReceivedPane(String key, String name, Client client) {
        super();
        this.setMessage(name + " muốn thách đấu bạn.");
        this.setMessageType(QUESTION_MESSAGE);
        this.setOptionType(YES_NO_OPTION);
        String[] options = {"Ok", "Hủy"};
        this.setOptions(options);
        this.key = key;
        this.client = client;
    }

    public void showOptionPane(Component parent) {
        dialog = this.createDialog(parent, "Invite");
        dialog.setVisible(true);
        dialog.dispose();
        if (getValue() == "Ok") {
            client.sendStringArray(new String[]{"join", "accept", key});
        } else if (getValue() == "Hủy") {
            client.sendStringArray(new String[]{"join", "reject", key});
        }
    }

    public void dispose() {
        dialog.dispose();
    }
}
