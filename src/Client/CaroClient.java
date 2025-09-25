package Client;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
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
    private JButton backToLobbyButton = new JButton("Về sảnh");
    private String myName = null;
    private boolean gameOver = false;
    private boolean offlineMode = false; // true when playing vs machine
    // Online players
    private DefaultListModel<String> onlineModel = new DefaultListModel<>();
    private JList<String> onlineList = new JList<>(onlineModel);
    private JButton challengeButton = new JButton("Thách đấu");
    private JPanel rightPanel; // holds online list and challenge button
    private JPanel lobbyPanel; // lobby screen
    private JPanel boardPanel; // game screen
    private JPanel menuPanel; // lobby menu buttons
    private CardLayout rootLayout = new CardLayout();
    private JPanel rootPanel = new JPanel(rootLayout);
    private JButton statsButton; // only visible in online mode

    public CaroClient() {
        // Ask for player name
        String playerName = JOptionPane.showInputDialog(this, "Nhập tên của bạn:", "Người chơi");
        if (playerName == null || playerName.trim().isEmpty())
            playerName = "Player" + (System.currentTimeMillis() % 1000);
        this.myName = playerName;
        setTitle("🎮 Caro Online - " + playerName);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 600);
        setLayout(new BorderLayout());

        // Set background color
        getContentPane().setBackground(new Color(240, 248, 255));

        boardPanel = new JPanel(new GridLayout(3, 3, 8, 8));
        boardPanel.setBackground(new Color(240, 248, 255));
        boardPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                buttons[i][j] = new JButton(" ");
                buttons[i][j].setFont(new Font("Arial", Font.BOLD, 60));
                buttons[i][j].setOpaque(true);
                buttons[i][j].setContentAreaFilled(true);
                buttons[i][j].setFocusPainted(false);
                buttons[i][j].setBorderPainted(true);
                buttons[i][j].setHorizontalAlignment(SwingConstants.CENTER);
                buttons[i][j].setVerticalAlignment(SwingConstants.CENTER);
                buttons[i][j].setIconTextGap(0);

                // Style the buttons
                buttons[i][j].setBackground(new Color(255, 255, 255));
                buttons[i][j].setBorder(BorderFactory.createRaisedBevelBorder());
                buttons[i][j].setForeground(new Color(70, 130, 180));

                int row = i, col = j;
                buttons[i][j].addActionListener(e -> makeMove(row, col));
                boardPanel.add(buttons[i][j]);
            }
        }

        lobbyPanel = new JPanel(new BorderLayout(15, 15));
        lobbyPanel.setBackground(new Color(240, 248, 255));
        lobbyPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Title panel
        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(new Color(240, 248, 255));
        JLabel titleLabel = new JLabel("🎮 CARO ONLINE", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(new Color(25, 25, 112));
        titlePanel.add(titleLabel);
        lobbyPanel.add(titlePanel, BorderLayout.NORTH);

        menuPanel = new JPanel(new GridLayout(4, 1, 15, 15));
        menuPanel.setBackground(new Color(240, 248, 255));
        menuPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));

        JButton btnFriend = createStyledButton("👥 Chơi với một người bạn", new Color(34, 139, 34));
        JButton btnRobot = createStyledButton("🤖 Chơi với máy", new Color(255, 140, 0));
        JButton btnQuickPlay = createStyledButton("⚡ Chơi trực tuyến", new Color(220, 20, 60));

        menuPanel.add(btnFriend);
        menuPanel.add(btnRobot);
        menuPanel.add(btnQuickPlay);
        lobbyPanel.add(menuPanel, BorderLayout.CENTER);

        // Right panel: online list + challenge button (online only)
        rightPanel = new JPanel(new BorderLayout(10, 10));
        rightPanel.setBackground(new Color(240, 248, 255));
        rightPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(70, 130, 180), 2),
            "👥 Người chơi online",
            0, 0,
            new Font("Arial", Font.BOLD, 14),
            new Color(25, 25, 112)
        ));

        onlineList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        onlineList.setBackground(new Color(255, 255, 255));
        onlineList.setFont(new Font("Arial", Font.PLAIN, 12));
        JScrollPane sc = new JScrollPane(onlineList);
        sc.setBackground(new Color(255, 255, 255));
        rightPanel.add(sc, BorderLayout.CENTER);

        // Create challengeButton THEN add listeners (fix ordering bug)
        challengeButton = createStyledButton("⚔️ Thách đấu", new Color(220, 20, 60));
        challengeButton.setEnabled(false); // default disabled until someone selected
        onlineList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String sel = onlineList.getSelectedValue();
                if (sel == null) {
                    challengeButton.setEnabled(false);
                    return;
                }
                String status = parseStatus(sel);
                challengeButton.setEnabled("free".equals(status));
            }
        });

        challengeButton.addActionListener(e -> {
            String sel = onlineList.getSelectedValue();
            if (sel != null && out != null) {
                String targetName = parseName(sel);
                String status = parseStatus(sel);
                if (!"free".equals(status)) {
                    JOptionPane.showMessageDialog(this, "Người chơi đang bận, hãy thử lại sau.");
                    return;
                }
                out.println("challenge:" + targetName);
                JOptionPane.showMessageDialog(this, "Đã gửi lời thách đấu tới " + targetName);
            } else if (out == null) {
                JOptionPane.showMessageDialog(this, "Chưa kết nối server.");
            }
        });

        rightPanel.add(challengeButton, BorderLayout.SOUTH);
        lobbyPanel.add(rightPanel, BorderLayout.EAST);

        // Stats button (online only)
        statsButton = createStyledButton("📊 Lịch sử", new Color(70, 130, 180));
        statsButton.addActionListener(e -> {
            if (out != null)
                out.println("get_stats");
            else
                JOptionPane.showMessageDialog(this, "Chưa kết nối server.");
        });
        lobbyPanel.add(statsButton, BorderLayout.SOUTH);

        // Root with cards
        rootPanel.add(lobbyPanel, "LOBBY");
        JPanel gameContainer = new JPanel(new BorderLayout());
        gameContainer.setBackground(new Color(240, 248, 255));
        gameContainer.add(boardPanel, BorderLayout.CENTER);

        // Style status label
        statusLabel.setFont(new Font("Arial", Font.BOLD, 18));
        statusLabel.setForeground(new Color(25, 25, 112));
        statusLabel.setBackground(new Color(255, 255, 255));
        statusLabel.setOpaque(true);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        gameContainer.add(statusLabel, BorderLayout.SOUTH);

        // Style action buttons
        JPanel topActions = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        topActions.setBackground(new Color(240, 248, 255));
        playAgainButton = createStyledButton("🔄 Chơi lại", new Color(34, 139, 34));
        backToLobbyButton = createStyledButton("🏠 Về sảnh", new Color(70, 130, 180));
        topActions.add(playAgainButton);
        topActions.add(backToLobbyButton);
        gameContainer.add(topActions, BorderLayout.NORTH);
        rootPanel.add(gameContainer, "GAME");
        add(rootPanel, BorderLayout.CENTER);

        // Play again button (hidden until game end)
        playAgainButton.setVisible(false);
        backToLobbyButton.setVisible(false);
        playAgainButton.addActionListener(e -> {
            if (out != null) {
                out.println("play_again");
                playAgainButton.setEnabled(false);
                statusLabel.setText("Đã gửi yêu cầu chơi lại, chờ đối thủ...");
            }
        });
        backToLobbyButton.addActionListener(e -> {
            // inform server we're leaving the match
            if (out != null) out.println("leave");
            // Return to lobby from game view
            rootLayout.show(rootPanel, "LOBBY");
            playAgainButton.setVisible(false);
            backToLobbyButton.setVisible(false);
            gameOver = false;
            clearBoard();
            statusLabel.setText("Đang chờ đối thủ...");
        });
        // Menu actions
        btnFriend.addActionListener(e -> {
            String friend = JOptionPane.showInputDialog(this, "Nhập tên người bạn muốn mời:");
            if (friend != null && !friend.trim().isEmpty()) {
                if (out != null) {
                    out.println("challenge:" + friend.trim());
                    JOptionPane.showMessageDialog(this, "Đã gửi lời mời tới " + friend.trim());
                } else {
                    JOptionPane.showMessageDialog(this, "Chưa kết nối server.");
                }
            }
        });
        btnRobot.addActionListener(e -> startOfflineGame());
        btnQuickPlay.addActionListener(e -> {
            String target = pickRandomFreeOpponent();
            if (target == null) {
                JOptionPane.showMessageDialog(this, "Không tìm thấy người chơi rảnh.");
                return;
            }
            if (out != null) {
                out.println("challenge:" + target);
                JOptionPane.showMessageDialog(this, "Đã gửi lời thách đấu tới " + target);
            } else {
                JOptionPane.showMessageDialog(this, "Chưa kết nối server.");
            }
        });

        setVisible(true);
        rootLayout.show(rootPanel, "LOBBY");
        connectToServer();
        clearBoard();
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
            // don't exit immediately — allow offline play
        }
    }

    private void handleServerMessage(String msg) {
        if (msg == null) return;
        if (msg.startsWith("online:")) {
            String payload = msg.substring(7);
            String[] items = payload.isEmpty() ? new String[0] : payload.split(",");
            onlineModel.clear();
            for (String it : items) {
                String[] pair = it.split("\\|", 2);
                String name = pair.length > 0 ? pair[0] : it;
                String status = pair.length > 1 ? pair[1] : "free";
                if (!name.equals(myName)) {
                    String display = name + ("busy".equals(status) ? " (bận)" : " (rảnh)");
                    onlineModel.addElement(display);
                }
            }
            String sel = onlineList.getSelectedValue();
            if (sel != null) {
                challengeButton.setEnabled("free".equals(parseStatus(sel)));
            } else {
                challengeButton.setEnabled(false);
            }
            return;
        }
        if (msg.startsWith("invite:")) {
            String from = msg.substring(7);
            int r = JOptionPane.showConfirmDialog(this, "Bạn nhận được lời mời từ " + from + ". Chấp nhận?", "Lời mời",
                    JOptionPane.YES_NO_OPTION);
            if (r == JOptionPane.YES_OPTION) {
                if (out != null)
                    out.println("accept:" + from);
            } else {
                if (out != null)
                    out.println("reject:" + from);
            }
            return;
        }
        if (msg.startsWith("rejected:")) {
            String by = msg.substring(9);
            JOptionPane.showMessageDialog(this, by + " đã từ chối lời mời.");
            return;
        }
        if (msg.startsWith("role:")) {
            myMark = msg.charAt(5);
            if (myMark == 'X') {
                statusLabel.setText("🎯 Lượt của bạn! (Bạn là X - Màu xanh)");
                myTurn = true;
            } else {
                statusLabel.setText("⏳ Đợi đối thủ... (Bạn là O - Màu đỏ)");
                myTurn = false;
            }
            updateButtonsEnabled();
            rootLayout.show(rootPanel, "GAME");
            playAgainButton.setVisible(false);
            backToLobbyButton.setVisible(false);
        } else if (msg.startsWith("board:")) {
            gameOver = false;
            String[] rows = msg.substring(6).split(";");
            playAgainButton.setVisible(false);
            playAgainButton.setEnabled(true);
            backToLobbyButton.setVisible(false);
            for (int i = 0; i < 3; i++) {
                String[] cols = rows.length > i ? rows[i].split("\\|") : new String[]{" ", " ", " "};
                for (int j = 0; j < 3; j++) {
                    char c = (cols.length > j && cols[j].length() > 0) ? cols[j].charAt(0) : ' ';
                    board[i][j] = c;
                    if (board[i][j] == ' ') {
                        buttons[i][j].setIcon(null);
                        buttons[i][j].setText(" ");
                    } else {
                        buttons[i][j].setText("");
                        Icon icon = createMarkIcon(board[i][j], 72);
                        buttons[i][j].setIcon(icon);
                        buttons[i][j].setDisabledIcon(icon);
                        buttons[i][j].revalidate();
                        buttons[i][j].repaint();
                    }
                    applyMarkToButton(i, j, board[i][j]);
                }
            }
            updateButtonsEnabled();
            printBoardToConsole();
            // if opponent sent rematch request earlier it will be visible when role arrives
        } else if (msg.startsWith("opponent:")) {
            String opp = msg.substring(9);
            setTitle("Caro TCP Client - vs " + opp);
            // remove opponent from online list while in-game
            for (int i = 0; i < onlineModel.size(); i++) {
                String item = onlineModel.get(i);
                if (item != null && item.startsWith(opp + " ")) {
                    onlineModel.removeElement(item);
                    break;
                }
            }
        } else if (msg.startsWith("stats:")) {
            String payload = msg.substring(6);
            String[] parts = payload.split(";");
            String wins = parts.length > 0 && parts[0].contains("=") ? parts[0].split("=")[1] : "0";
            String losses = parts.length > 1 && parts[1].contains("=") ? parts[1].split("=")[1] : "0";
            String draws = parts.length > 2 && parts[2].contains("=") ? parts[2].split("=")[1] : "0";
            String rate = parts.length > 3 && parts[3].contains("=") ? parts[3].split("=")[1] : "0%";
            JOptionPane.showMessageDialog(this, "Thống kê của bạn:\nThắng: " + wins + "\nThua: " + losses + "\nHòa: "
                    + draws + "\nTỷ lệ thắng: " + rate);
        } else if (msg.equals("your_turn")) {
            myTurn = true;
            statusLabel.setText("🎯 Lượt của bạn!");
            updateButtonsEnabled();
        } else if (msg.equals("wait")) {
            myTurn = false;
            statusLabel.setText("⏳ Đợi đối thủ...");
            updateButtonsEnabled();
        } else if (msg.equals("invalid_move")) {
            statusLabel.setText("Nước đi không hợp lệ, hãy thử lại.");
            myTurn = true;
            updateButtonsEnabled();
        } else if (msg.equals("win")) {
            statusLabel.setText("🎉 Bạn thắng! Chúc mừng!");
            gameOver = true;
            disableButtons();
            playAgainButton.setVisible(true);
            backToLobbyButton.setVisible(true);
        } else if (msg.equals("lose")) {
            statusLabel.setText("😢 Bạn thua! Thử lại nhé!");
            gameOver = true;
            disableButtons();
            playAgainButton.setVisible(true);
            backToLobbyButton.setVisible(true);
        } else if (msg.equals("draw")) {
            statusLabel.setText("🤝 Hòa! Cả hai đều giỏi!");
            gameOver = true;
            disableButtons();
            playAgainButton.setVisible(true);
            backToLobbyButton.setVisible(true);
        } else if (msg.startsWith("opponent_left:")) {
            String who = msg.substring(14);
            JOptionPane.showMessageDialog(this, "Đối thủ " + who + " đã rời. Trận kết thúc.");
            rootLayout.show(rootPanel, "LOBBY");
            playAgainButton.setVisible(false);
            backToLobbyButton.setVisible(false);
        } else if (msg.equals("opponent_wants_rematch")) {
            statusLabel.setText("Đối thủ muốn chơi lại...");
            playAgainButton.setVisible(true);
            playAgainButton.setEnabled(true);
            return;
        } else if (msg.startsWith("error:")) {
            String e = msg.substring(6);
            if ("player_busy".equals(e)) {
                JOptionPane.showMessageDialog(this, "Người chơi đang bận, hãy thử lại sau.");
            } else if ("you_are_busy".equals(e)) {
                JOptionPane.showMessageDialog(this, "Bạn đang trong trận/đang bận.");
            } else {
                JOptionPane.showMessageDialog(this, "Lỗi server: " + e);
            }
        }
    }

    private void makeMove(int row, int col) {
        if (gameOver)
            return;
        if (!myTurn)
            return;
        if (board[row][col] != ' ')
            return;

        board[row][col] = myMark;
        buttons[row][col].setText("");
        Icon icon = createMarkIcon(myMark, 72);
        buttons[row][col].setIcon(icon);
        buttons[row][col].setDisabledIcon(icon);
        buttons[row][col].revalidate();
        buttons[row][col].repaint();
        applyMarkToButton(row, col, myMark);

        if (!offlineMode) {
            if (out != null) out.println("move:" + row + "," + col);
            myTurn = false;
            statusLabel.setText("Đợi đối thủ...");
            updateButtonsEnabled();
            return;
        }

        // offline AI logic (unchanged)
        if (checkWin(myMark)) {
            statusLabel.setText("🎉 Bạn thắng máy! Xuất sắc!");
            gameOver = true;
            disableButtons();
            playAgainButton.setVisible(true);
            backToLobbyButton.setVisible(true);
            return;
        }
        if (isDraw()) {
            statusLabel.setText("🤝 Hòa với máy! Khá đấy!");
            gameOver = true;
            disableButtons();
            playAgainButton.setVisible(true);
            backToLobbyButton.setVisible(true);
            return;
        }
        myTurn = false;
        statusLabel.setText("🤖 Máy đang suy nghĩ...");
        Point ai = pickAiMove();
        if (ai != null) {
            char aiMark = (myMark == 'X') ? 'O' : 'X';
            board[ai.x][ai.y] = aiMark;
            buttons[ai.x][ai.y].setText("");
            Icon aiIcon = createMarkIcon(aiMark, 72);
            buttons[ai.x][ai.y].setIcon(aiIcon);
            buttons[ai.x][ai.y].setDisabledIcon(aiIcon);
            buttons[ai.x][ai.y].revalidate();
            buttons[ai.x][ai.y].repaint();
            applyMarkToButton(ai.x, ai.y, aiMark);
            if (checkWin(aiMark)) {
                statusLabel.setText("😢 Máy thắng! Thử lại nhé!");
                gameOver = true;
                disableButtons();
                playAgainButton.setVisible(true);
                backToLobbyButton.setVisible(true);
                return;
            }
        }
        if (isDraw()) {
            statusLabel.setText("🤝 Hòa với máy! Khá đấy!");
            gameOver = true;
            disableButtons();
            playAgainButton.setVisible(true);
            backToLobbyButton.setVisible(true);
            return;
        }
        myTurn = true;
        statusLabel.setText("🎯 Lượt của bạn!");
        updateButtonsEnabled();
    }

    private void updateButtonsEnabled() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                boolean shouldEnable = (!gameOver) && (board[i][j] == ' ') && myTurn;
                buttons[i][j].setEnabled(shouldEnable);
            }
        }
    }

    private void disableButtons() {
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                buttons[i][j].setEnabled(false);
    }

    private void printBoardToConsole() {
        for (int i = 0; i < 3; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < 3; j++) {
                char c = board[i][j];
                sb.append('[').append(c).append(']');
                if (j < 2)
                    sb.append(' ');
            }
            System.out.println(sb.toString());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(CaroClient::new);
    }

    // offline helpers
    private void clearBoard() {
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++) {
                board[i][j] = ' ';
                buttons[i][j].setIcon(null);
                buttons[i][j].setText(" ");
                applyMarkToButton(i, j, ' ');
            }
    }

    private void startOfflineGame() {
        offlineMode = true;
        clearBoard();
        myMark = 'X';
        myTurn = true;
        statusLabel.setText("Lượt của bạn! (Bạn là X)");
        playAgainButton.setVisible(false);
        backToLobbyButton.setVisible(false);
        rootLayout.show(rootPanel, "GAME");
        updateButtonsEnabled();
    }

    private String pickRandomFreeOpponent() {
        for (int i = 0; i < onlineModel.size(); i++) {
            String item = onlineModel.get(i);
            if ("free".equals(parseStatus(item))) {
                return parseName(item);
            }
        }
        return null;
    }

    private String parseName(String display) {
        if (display == null) return "";
        int idx = display.indexOf(" (");
        return idx > 0 ? display.substring(0, idx) : display;
    }

    private String parseStatus(String display) {
        if (display == null) return "free";
        if (display.contains("(bận)")) return "busy";
        if (display.contains("(rảnh)")) return "free";
        return "free";
    }

    // game logic helpers unchanged...
    private boolean checkWin(char mark) {
        for (int i = 0; i < 3; i++) {
            if (board[i][0] == mark && board[i][1] == mark && board[i][2] == mark) return true;
            if (board[0][i] == mark && board[1][i] == mark && board[2][i] == mark) return true;
        }
        if (board[0][0] == mark && board[1][1] == mark && board[2][2] == mark) return true;
        if (board[0][2] == mark && board[1][1] == mark && board[2][0] == mark) return true;
        return false;
    }

    private boolean isDraw() {
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                if (board[i][j] == ' ')
                    return false;
        return true;
    }

    private java.awt.Point pickAiMove() {
        if (board[1][1] == ' ') return new java.awt.Point(1, 1);
        int[][] pref = { {0,0},{0,2},{2,0},{2,2},{0,1},{1,0},{1,2},{2,1} };
        for (int[] p : pref) {
            if (board[p[0]][p[1]] == ' ') return new java.awt.Point(p[0], p[1]);
        }
        return null;
    }

    private Icon createMarkIcon(char mark, int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) img.getGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(0, 0, 0, 0));
            g.fillRect(0, 0, size, size);
            g.setFont(new Font("Arial", Font.BOLD, size - 6));
            String s = String.valueOf(mark);
            FontMetrics fm = g.getFontMetrics();
            int w = fm.stringWidth(s);
            int h = fm.getAscent();
            int x = (size - w) / 2;
            int y = (size + h) / 2 - 4;

            if (mark == 'X') {
                g.setColor(new Color(0, 150, 0)); // Bright green
                g.drawString(s, x, y);
            } else if (mark == 'O') {
                g.setColor(new Color(200, 0, 0)); // Bright red
                g.drawString(s, x, y);
            } else {
                g.setColor(Color.BLACK);
                g.drawString(s, x, y);
            }
        } finally {
            g.dispose();
        }
        return new ImageIcon(img);
    }

    private void applyMarkToButton(int row, int col, char mark) {
        if (mark == 'X') {
            buttons[row][col].setForeground(Color.GREEN.darker());
        } else if (mark == 'O') {
            buttons[row][col].setForeground(Color.RED.darker());
        } else {
            buttons[row][col].setForeground(Color.BLACK);
        }
    }

    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(true);
        button.setBorder(BorderFactory.createRaisedBevelBorder());
        button.setPreferredSize(new Dimension(200, 40));
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(color.darker());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(color);
            }
        });
        return button;
    }
}
