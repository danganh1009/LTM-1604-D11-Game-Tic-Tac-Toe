package Client;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class CaroClient extends JFrame {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private char myMark = ' ';
    private char[][] board = new char[3][3];
    private JButton[][] buttons = new JButton[3][3];
    private JLabel statusLabel = new JLabel("Đang chờ đối thủ...", SwingConstants.CENTER);
    private boolean myTurn = false;
    private JButton playAgainButton = new JButton("Chơi lại");
    private String myName = null;

    public CaroClient() {
        // Ask for player name
        String playerName = JOptionPane.showInputDialog(this, "Nhập tên của bạn:", "Người chơi");
        if (playerName == null || playerName.trim().isEmpty())
            playerName = "Player" + (System.currentTimeMillis() % 1000);
        this.myName = playerName;
        setTitle("Caro TCP Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 500);
        setLayout(new BorderLayout());

        JPanel boardPanel = new JPanel(new GridLayout(3, 3, 5, 5));
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                buttons[i][j] = new JButton(" ");
                buttons[i][j].setFont(new Font("Arial", Font.BOLD, 60));
                int row = i, col = j;
                buttons[i][j].addActionListener(e -> makeMove(row, col));
                boardPanel.add(buttons[i][j]);
            }
        }
        add(boardPanel, BorderLayout.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 18));
        add(statusLabel, BorderLayout.SOUTH);

        // Play again button (hidden until game end)
        playAgainButton.setVisible(false);
        playAgainButton.addActionListener(e -> {
            if (out != null) {
                out.println("play_again");
                playAgainButton.setEnabled(false);
                statusLabel.setText("Đã gửi yêu cầu chơi lại, chờ đối thủ...");
            }
        });
        add(playAgainButton, BorderLayout.NORTH);

        // Stats button
        JButton statsButton = new JButton("Lịch sử");
        statsButton.addActionListener(e -> {
            if (out != null)
                out.println("get_stats");
        });
        add(statsButton, BorderLayout.EAST);

        setVisible(true);

        connectToServer();
    }

    private void connectToServer() {
        try {
            socket = new Socket("localhost", 8000);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // send login name if available
            if (myName != null && !myName.isEmpty()) {
                out.println("login:" + myName);
            }

            new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        final String msg = line;
                        SwingUtilities.invokeLater(() -> handleServerMessage(msg));
                    }
                } catch (IOException e) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Mất kết nối server!"));
                    System.exit(0);
                }
            }).start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Không kết nối được server!");
            System.exit(0);
        }
    }

    private void handleServerMessage(String msg) {
        if (msg.startsWith("role:")) {
            myMark = msg.charAt(5);
            if (myMark == 'X') {
                statusLabel.setText("Lượt của bạn! (Bạn là X)");
                myTurn = true;
            } else {
                statusLabel.setText("Đợi đối thủ... (Bạn là O)");
                myTurn = false;
            }
            updateButtonsEnabled();
        } else if (msg.startsWith("board:")) {
            String[] rows = msg.substring(6).split(";");
            // when a new board arrives at the start of a game, hide the play again button
            playAgainButton.setVisible(false);
            playAgainButton.setEnabled(true);
            for (int i = 0; i < 3; i++) {
                String[] cols = rows[i].split("\\|");
                for (int j = 0; j < 3; j++) {
                    board[i][j] = cols[j].charAt(0);
                    buttons[i][j].setText(board[i][j] == ' ' ? " " : String.valueOf(board[i][j]));
                    if (board[i][j] == 'X')
                        buttons[i][j].setForeground(Color.GREEN.darker());
                    else if (board[i][j] == 'O')
                        buttons[i][j].setForeground(Color.RED.darker());
                    else
                        buttons[i][j].setForeground(Color.BLACK);
                    // visually mark occupied cells with background
                    if (board[i][j] != ' ') {
                        buttons[i][j].setBackground(new Color(230, 230, 230));
                    } else {
                        buttons[i][j].setBackground(UIManager.getColor("Button.background"));
                    }
                }
            }
            updateButtonsEnabled();
            // debug print board to console
            printBoardToConsole();
        } else if (msg.startsWith("opponent:")) {
            String opp = msg.substring(9);
            setTitle("Caro TCP Client - vs " + opp);
        } else if (msg.startsWith("stats:")) {
            String payload = msg.substring(6);
            // payload format wins=...;losses=...;draws=...;rate=...
            String[] parts = payload.split(";");
            String wins = parts.length > 0 ? parts[0].split("=")[1] : "0";
            String losses = parts.length > 1 ? parts[1].split("=")[1] : "0";
            String draws = parts.length > 2 ? parts[2].split("=")[1] : "0";
            String rate = parts.length > 3 ? parts[3].split("=")[1] : "0%";
            JOptionPane.showMessageDialog(this, "Thống kê của bạn:\nThắng: " + wins + "\nThua: " + losses + "\nHòa: "
                    + draws + "\nTỷ lệ thắng: " + rate);
        } else if (msg.equals("your_turn")) {
            myTurn = true;
            statusLabel.setText("Lượt của bạn!");
            updateButtonsEnabled();
        } else if (msg.equals("wait")) {
            myTurn = false;
            statusLabel.setText("Đợi đối thủ...");
            updateButtonsEnabled();
        } else if (msg.equals("invalid_move")) {
            // server told us the last move was invalid — re-enable so player can retry
            statusLabel.setText("Nước đi không hợp lệ, hãy thử lại.");
            myTurn = true;
            updateButtonsEnabled();
        } else if (msg.equals("win")) {
            statusLabel.setText("Bạn thắng!");
            disableButtons();
            playAgainButton.setVisible(true);
        } else if (msg.equals("lose")) {
            statusLabel.setText("Bạn thua!");
            disableButtons();
            playAgainButton.setVisible(true);
        } else if (msg.equals("draw")) {
            statusLabel.setText("Hòa!");
            disableButtons();
            playAgainButton.setVisible(true);
        }
    }

    private void makeMove(int row, int col) {
        if (!myTurn) {
            return;
        }
        if (board[row][col] != ' ') {
            return;
        }
        String msg = "move:" + row + "," + col;
        out.println(msg);
        myTurn = false;
        statusLabel.setText("Đợi đối thủ...");
        updateButtonsEnabled();
    }

    private void updateButtonsEnabled() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                boolean enable = myTurn && board[i][j] == ' ';
                buttons[i][j].setEnabled(enable);
            }
        }
        logEnabledButtons();
    }

    private void disableButtons() {
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                buttons[i][j].setEnabled(false);
    }

    // Debug helper: print current board array to console
    private void printBoardToConsole() {
        // debug: board state (silent)
        for (int i = 0; i < 3; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < 3; j++) {
                char c = board[i][j];
                sb.append('[').append(c).append(']');
                if (j < 2)
                    sb.append(' ');
            }
            // print board row (silent)
        }
    }

    // Debug helper: print enabled state of each button
    private void logEnabledButtons() {
        StringBuilder sb = new StringBuilder();
        sb.append("Buttons enabled state: ");
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                sb.append(buttons[i][j].isEnabled() ? "1" : "0");
                if (j < 2)
                    sb.append(',');
            }
            if (i < 2)
                sb.append(" | ");
        }
        // buttons enabled state (silent)
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(CaroClient::new);
    }
}