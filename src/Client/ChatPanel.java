package Client;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ChatPanel extends JPanel {
    private JTextPane chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    private StyledDocument doc;
    private Style timeStyle;
    private Style systemStyle;
    private Style userStyle;
    private Style opponentStyle;

    public ChatPanel() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createTitledBorder("Chat"));

        // Chat area
        chatArea = new JTextPane();
        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setPreferredSize(new Dimension(200, 150));

        // Message input
        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        messageField = new JTextField();
        sendButton = new JButton("Gửi");
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        add(scrollPane, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);

        // Initialize styles
        doc = chatArea.getStyledDocument();

        timeStyle = chatArea.addStyle("Time", null);
        StyleConstants.setForeground(timeStyle, Color.GRAY);
        StyleConstants.setFontSize(timeStyle, 10);

        systemStyle = chatArea.addStyle("System", null);
        StyleConstants.setForeground(systemStyle, Color.BLUE);
        StyleConstants.setBold(systemStyle, true);

        userStyle = chatArea.addStyle("User", null);
        StyleConstants.setForeground(userStyle, new Color(0, 100, 0));
        StyleConstants.setBold(userStyle, true);

        opponentStyle = chatArea.addStyle("Opponent", null);
        StyleConstants.setForeground(opponentStyle, new Color(150, 0, 0));
        StyleConstants.setBold(opponentStyle, true);
    }

    public void setMessageHandler(MessageHandler handler) {
        sendButton.addActionListener(e -> sendMessage(handler));
        messageField.addActionListener(e -> sendMessage(handler));
    }

    private void sendMessage(MessageHandler handler) {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            handler.handleMessage(message);
            messageField.setText("");
        }
    }

    public void addSystemMessage(String message) {
        appendMessage("[Hệ thống] " + message, systemStyle);
    }

    public void addUserMessage(String username, String message) {
        appendMessage(username + ": " + message, userStyle);
    }

    public void addOpponentMessage(String username, String message) {
        appendMessage(username + ": " + message, opponentStyle);
    }

    private void appendMessage(String message, Style style) {
        try {
            String time = "[" + timeFormat.format(new Date()) + "] ";
            doc.insertString(doc.getLength(), time, timeStyle);
            doc.insertString(doc.getLength(), message + "\n", style);
            chatArea.setCaretPosition(doc.getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    public void clear() {
        try {
            doc.remove(0, doc.getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    public interface MessageHandler {
        void handleMessage(String message);
    }
}