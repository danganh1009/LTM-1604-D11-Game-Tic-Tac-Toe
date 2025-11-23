package Client;

import javax.swing.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.Random;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.event.*;
import javax.swing.border.*;

public class CaroClient extends JFrame {
	private Socket socket;
	private PrintWriter out;
	private BufferedReader in;
	private char myMark = ' ';
	private char[][] board = new char[3][3];
	private boolean isMatchmaking = false; // Biến kiểm soát trạng thái ghép trận
	private boolean isInGame = false; // Biến kiểm soát trạng thái đang trong game
	private JButton[][] buttons = new JButton[3][3];
	private JLabel statusLabel = new JLabel("Đang chờ đối thủ...", SwingConstants.CENTER);
	private JLabel modeLabel = new JLabel("", SwingConstants.CENTER);
	private boolean myTurn = false;
	private JButton playAgainButton = new JButton("Đấu lại");
	private JButton backToLobbyButton = new JButton("Về sảnh");
	private JButton cancelMatchButton = new JButton("Hủy ghép trận");
	private JButton statsButton = new JButton("Bảng xếp hạng");
	private JButton historyButton = new JButton("Lịch sử đấu");
	private JDialog leaderboardDialog;
	private JTable leaderboardTable;
	private DefaultTableModel leaderboardModel;
	private String myName = null;
	private JLabel userAvatarLabel;
	private JLabel userNameLabel;

	// Matchmaking dialog
	private JDialog matchmakingDialog;
	private JLabel matchmakingLabel;
	private boolean gameOver = false;
	private boolean offlineMode = false;
	private volatile boolean running = true;
	private Thread readerThread;
	private DefaultListModel<String> onlineModel = new DefaultListModel<>();
	private JList<String> onlineList = new JList<>(onlineModel);
	private JButton challengeButton = new JButton("Thách đấu");
	private JPanel rightPanel;
	private JPanel lobbyPanel;
	private JPanel boardPanel;
	private JPanel menuPanel;
	private CardLayout rootLayout = new CardLayout();
	private JPanel rootPanel = new JPanel(rootLayout);

	private volatile boolean aiThinking = false;
	private String pendingPassword;

	public CaroClient() {
		this.myName = null; // Bắt buộc đăng nhập qua dialog
		if (this.myName == null || this.myName.trim().isEmpty()) {
			// If user closes dialog, set temporary name but don't login to server
			this.myName = "Player" + (System.currentTimeMillis() % 1000);
		}
		setTitle("Cờ Caro 3x3 - " + this.myName);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(800, 600); // Kích thước lớn hơn
		setLayout(new BorderLayout(15, 15)); // Thêm padding

		// Thiết lập giao diện chính với gradient background
		JPanel mainPanel = UIEffects.createGradientPanel(
				new Color(240, 248, 255).brighter(),
				new Color(240, 248, 255),
				true);
		mainPanel.setLayout(new BorderLayout(15, 15));
		mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		setContentPane(mainPanel);

		// Game Board Panel with modern design and effects
		boardPanel = new JPanel(new GridLayout(3, 3, 8, 8)) {
			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2d = (Graphics2D) g.create();
				g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

				// Nền gradient tối với ánh sáng nhẹ
				GradientPaint bgGradient = new GradientPaint(
						0, 0, new Color(18, 24, 33),
						getWidth(), getHeight(), new Color(13, 17, 23));
				g2d.setPaint(bgGradient);
				g2d.fillRect(0, 0, getWidth(), getHeight());

				// Thêm hiệu ứng bóng mờ cho nền
				g2d.setColor(new Color(255, 255, 255, 10));
				g2d.fillRect(0, 0, getWidth(), 2);
				g2d.fillRect(0, 0, 2, getHeight());

				// Vẽ các đường lưới phát sáng
				// Màu chính cho lưới
				g2d.setColor(new Color(48, 54, 61));
				g2d.setStroke(new BasicStroke(2f));

				// Vẽ các đường dọc
				int cellWidth = getWidth() / 3;
				for (int i = 1; i < 3; i++) {
					int x = i * cellWidth;
					g2d.drawLine(x, 0, x, getHeight());

					// Thêm hiệu ứng sáng cho lưới
					g2d.setColor(new Color(255, 255, 255, 15));
					g2d.drawLine(x + 1, 0, x + 1, getHeight());
					g2d.setColor(new Color(48, 54, 61));
				}

				// Vẽ các đường ngang
				int cellHeight = getHeight() / 3;
				for (int i = 1; i < 3; i++) {
					int y = i * cellHeight;
					g2d.drawLine(0, y, getWidth(), y);

					// Thêm hiệu ứng sáng cho lưới
					g2d.setColor(new Color(255, 255, 255, 15));
					g2d.drawLine(0, y + 1, getWidth(), y + 1);
					g2d.setColor(new Color(48, 54, 61));
				}
				g2d.dispose();
			}
		};
		boardPanel.setOpaque(false);
		boardPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

		// Tạo các ô cờ
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				buttons[i][j] = new JButton(" ") {
					private float glowIntensity = 0f;
					private Timer glowTimer;
					{
						glowTimer = new Timer(50, e -> {
							if (isEnabled() && getText().equals(" ")) {
								glowIntensity = (float) (Math.sin(System.currentTimeMillis() / 1000.0) + 1) / 2;
								repaint();
							}
						});
						glowTimer.start();
					}

					@Override
					protected void paintComponent(Graphics g) {
						Graphics2D g2d = (Graphics2D) g.create();
						g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
						g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

						// Nền nút với gradient và hiệu ứng phát sáng nhẹ
						GradientPaint buttonGradient = new GradientPaint(
								0, 0, new Color(22, 27, 34),
								getWidth(), getHeight(), new Color(17, 21, 26));
						g2d.setPaint(buttonGradient);
						g2d.fillRect(0, 0, getWidth(), getHeight());

						// Thêm viền sáng nhẹ
						if (isEnabled() && getText().equals(" ")) {
							g2d.setColor(new Color(255, 255, 255, (int) (20 * glowIntensity)));
							g2d.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
						}

						// Hiệu ứng phát sáng cho nút trống
						if (isEnabled() && getText().equals(" ")) {
							// Vẽ đường viền mờ
							g2d.setColor(new Color(48, 54, 61));
							g2d.setStroke(new BasicStroke(2f));
							g2d.drawRect(1, 1, getWidth() - 3, getHeight() - 3);
						}

						// Vẽ nội dung (X hoặc O)
						// Vẽ nội dung (X hoặc O) với kiểu hiện đại
						if (!getText().equals(" ")) {
							int size = Math.min(getWidth(), getHeight()) - 40; // Để lại margin
							int x = (getWidth() - size) / 2;
							int y = (getHeight() - size) / 2;

							g2d.setStroke(new BasicStroke(8.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
							g2d.setColor(Color.WHITE);

							if (getText().equals("X")) {
								// Vẽ X bằng 2 đường chéo
								g2d.drawLine(x, y, x + size, y + size);
								g2d.drawLine(x + size, y, x, y + size);
							} else if (getText().equals("O")) {
								// Vẽ O bằng hình tròn với stroke dày hơn
								g2d.setStroke(new BasicStroke(10.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
								g2d.drawOval(x + 5, y + 5, size - 10, size - 10);
							}
						}
						g2d.dispose();
					}

					@Override
					protected void paintBorder(Graphics g) {
						// Don't paint the default border
					}
				};

				buttons[i][j].setFont(new Font("Arial", Font.BOLD, 48));
				buttons[i][j].setForeground(Color.WHITE);
				buttons[i][j].setBackground(new Color(22, 27, 34));
				buttons[i][j].setOpaque(true);
				buttons[i][j].setBorderPainted(false);
				buttons[i][j].setFocusPainted(false);
				buttons[i][j].setBorder(null);

				// Thêm hiệu ứng hover
				int row = i, col = j;
				buttons[i][j].addMouseListener(new MouseAdapter() {
					@Override
					public void mouseEntered(MouseEvent e) {
						if (!gameOver && board[row][col] == ' ' && myTurn) {
							buttons[row][col].setBackground(new Color(230, 240, 250));
							buttons[row][col].setCursor(new Cursor(Cursor.HAND_CURSOR));
						}
					}

					@Override
					public void mouseExited(MouseEvent e) {
						buttons[row][col].setBackground(new Color(255, 255, 255, 0));
					}
				});

				buttons[i][j].addActionListener(e -> makeMove(row, col));
				boardPanel.add(buttons[i][j]);
			}
		}

		// Lobby Panel
		lobbyPanel = new JPanel(new BorderLayout(15, 15));
		lobbyPanel.setBackground(new Color(240, 248, 255));
		lobbyPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

		JPanel titlePanel = new JPanel(new BorderLayout());
		titlePanel.setBackground(new Color(240, 248, 255));

		JLabel titleLabel = new JLabel("CỜ CARO 3x3", SwingConstants.CENTER);
		titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
		titleLabel.setForeground(new Color(25, 25, 112));
		titlePanel.add(titleLabel, BorderLayout.CENTER);

		// User area (avatar + name) on the right
		JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
		userPanel.setOpaque(false);
		userAvatarLabel = new JLabel();
		userAvatarLabel.setPreferredSize(new Dimension(40, 40));
		userNameLabel = new JLabel(this.myName != null ? this.myName : "Guest");
		userNameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
		userNameLabel.setForeground(new Color(25, 25, 112));
		userPanel.add(userNameLabel);
		userPanel.add(userAvatarLabel);

		// Initial avatar
		userAvatarLabel.setIcon(makeAvatarIcon(this.myName, 40));

		// Popup menu for account actions (only logout)
		JPopupMenu accountMenu = new JPopupMenu();
		JMenuItem logout = new JMenuItem("Đăng xuất");
		accountMenu.add(logout);

		// Show menu when clicking avatar or name
		MouseAdapter showMenu = new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				accountMenu.show(e.getComponent(), e.getX(), e.getY());
			}
		};
		userAvatarLabel.addMouseListener(showMenu);
		userNameLabel.addMouseListener(showMenu);

		// Note: change-password option removed per user request; only logout remains

		// Logout action
		logout.addActionListener(ae -> {
			int ok = JOptionPane.showConfirmDialog(CaroClient.this, "Bạn có chắc muốn đăng xuất?", "Đăng xuất",
					JOptionPane.YES_NO_OPTION);
			if (ok == JOptionPane.YES_OPTION) {
				if (out != null)
					out.println("logout");
				myName = null;
				userNameLabel.setText("(Đã đăng xuất)");
				userAvatarLabel.setIcon(makeAvatarIcon(null, 40));
				// Close any auxiliary dialogs so only login appears
				try {
					if (leaderboardDialog != null)
						leaderboardDialog.dispose();
				} catch (Exception ignore) {
				}
				try {
					if (matchmakingDialog != null)
						matchmakingDialog.dispose();
				} catch (Exception ignore) {
				}
				// Hide main window and show login dialog
				setVisible(false);
				SwingUtilities.invokeLater(() -> showLoginDialog());
			}
		});

		titlePanel.add(userPanel, BorderLayout.EAST);
		lobbyPanel.add(titlePanel, BorderLayout.NORTH);

		menuPanel = new JPanel(new GridLayout(4, 1, 15, 15));
		menuPanel.setBackground(new Color(240, 248, 255));
		menuPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));

		JButton btnFriend = createStyledButton("Chơi với bạn", new Color(34, 139, 34));
		JButton btnRobot = createStyledButton("Chơi với máy", new Color(255, 140, 0));
		JButton btnQuickPlay = createStyledButton("Chơi trực tuyến", new Color(220, 20, 60));

		menuPanel.add(btnFriend);
		menuPanel.add(btnRobot);
		menuPanel.add(btnQuickPlay);
		lobbyPanel.add(menuPanel, BorderLayout.CENTER);

		// Right Panel: Online List + Challenge Button
		rightPanel = new JPanel(new BorderLayout(10, 10));
		rightPanel.setBackground(new Color(240, 248, 255));
		rightPanel.setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder(new Color(70, 130, 180), 2),
				"NGƯỜI CHƠI",
				0, 0,
				new Font("Arial", Font.BOLD, 14),
				new Color(25, 25, 112)));

		onlineList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		onlineList.setBackground(new Color(255, 255, 255));
		onlineList.setFont(new Font("Arial", Font.PLAIN, 12));
		JScrollPane sc = new JScrollPane(onlineList);
		sc.setBackground(new Color(255, 255, 255));
		rightPanel.add(sc, BorderLayout.CENTER);

		challengeButton = createStyledButton("Thách đấu", new Color(220, 20, 60));
		challengeButton.setEnabled(false);
		onlineList.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				String sel = onlineList.getSelectedValue();
				challengeButton.setEnabled(sel != null && "free".equals(parseStatus(sel)));
			}
		});
		challengeButton.addActionListener(e -> {
			String sel = onlineList.getSelectedValue();
			if (sel != null && out != null) {
				String targetName = parseName(sel);
				out.println("challenge:" + targetName);
				offlineMode = false; // Đảm bảo chế độ online khi thách đấu
				statusLabel.setText("Đang chờ " + targetName + " chấp nhận thách đấu...");
			} else if (out == null) {
				JOptionPane.showMessageDialog(this, "Chưa kết nối server.");
			}
		});
		rightPanel.add(challengeButton, BorderLayout.SOUTH);
		lobbyPanel.add(rightPanel, BorderLayout.EAST);

		// Stats Button
		statsButton = createStyledButton("Bảng xếp hạng top cao thủ", new Color(70, 130, 180));
		statsButton.addActionListener(e -> {
			if (out != null) {
				out.println("get_top");
			} else {
				JOptionPane.showMessageDialog(this, "Chưa kết nối server.");
			}
		});
		// History button
		historyButton = createStyledButton("Lịch sử đấu", new Color(127, 140, 141));
		historyButton.addActionListener(e -> showHistoryDialog());

		JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
		bottomPanel.setOpaque(false);
		bottomPanel.add(statsButton);
		bottomPanel.add(historyButton);
		lobbyPanel.add(bottomPanel, BorderLayout.SOUTH);

		// Game Container với hiệu ứng trong suốt và gradient
		JPanel gameContainer = new JPanel() {
			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2d = (Graphics2D) g.create();
				g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

				// Vẽ gradient background
				GradientPaint gp = new GradientPaint(
						0, 0, new Color(255, 255, 255, 240),
						0, getHeight(), new Color(240, 248, 255, 240));
				g2d.setPaint(gp);
				g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 25, 25);

				// Thêm viền với gradient
				GradientPaint borderGp = new GradientPaint(
						0, 0, new Color(70, 130, 180, 100),
						0, getHeight(), new Color(70, 130, 180, 150));
				g2d.setPaint(borderGp);
				g2d.setStroke(new BasicStroke(2f));
				g2d.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 25, 25);

				// Thêm hiệu ứng bóng mờ
				g2d.setColor(new Color(0, 0, 0, 10));
				g2d.drawRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 25, 25);
				g2d.dispose();
			}
		};
		gameContainer.setLayout(new BorderLayout(20, 20));
		gameContainer.setOpaque(false);
		gameContainer.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
		gameContainer.setPreferredSize(new Dimension(800, 600));
		// Wrap board with a small header that shows the current mode above the board
		JPanel boardWrapper = new JPanel(new BorderLayout());
		boardWrapper.setOpaque(false);
		JPanel boardHeader = new JPanel(new BorderLayout());
		boardHeader.setOpaque(false);
		modeLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
		modeLabel.setForeground(new Color(52, 73, 94));
		boardHeader.add(modeLabel, BorderLayout.CENTER);
		boardWrapper.add(boardHeader, BorderLayout.NORTH);
		boardWrapper.add(boardPanel, BorderLayout.CENTER);
		gameContainer.add(boardWrapper, BorderLayout.CENTER);

		// Status label with enhanced styling
		statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
		statusLabel.setForeground(new Color(41, 128, 185));
		statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
		statusLabel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(new Color(70, 130, 180, 180), 2, true),
				BorderFactory.createEmptyBorder(15, 25, 15, 25)));

		// Status panel with modern gradient and effects
		JPanel statusPanel = new JPanel(new BorderLayout()) {
			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2d = (Graphics2D) g.create();
				g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

				// Modern gradient background
				GradientPaint gp = new GradientPaint(
						0, 0, new Color(255, 255, 255, 240),
						0, getHeight(), new Color(240, 248, 255, 240));
				g2d.setPaint(gp);
				g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);

				// Subtle border gradient
				GradientPaint borderGp = new GradientPaint(
						0, 0, new Color(70, 130, 180, 100),
						0, getHeight(), new Color(70, 130, 180, 80));
				g2d.setPaint(borderGp);
				g2d.setStroke(new BasicStroke(2f));
				g2d.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 20, 20);

				g2d.dispose();
			}
		};
		statusPanel.setOpaque(false);
		statusPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));
		statusPanel.add(statusLabel, BorderLayout.CENTER);
		gameContainer.add(statusPanel, BorderLayout.SOUTH);

		// Action buttons panel with enhanced design
		JPanel topActions = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 12)) {
			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2d = (Graphics2D) g.create();
				g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

				// Modern semi-transparent gradient background
				GradientPaint gp = new GradientPaint(
						0, 0, new Color(255, 255, 255, 230),
						0, getHeight(), new Color(240, 248, 255, 230));
				g2d.setPaint(gp);
				g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);

				// Elegant border with gradient
				GradientPaint borderGp = new GradientPaint(
						0, 0, new Color(70, 130, 180, 100),
						0, getHeight(), new Color(70, 130, 180, 80));
				g2d.setPaint(borderGp);
				g2d.setStroke(new BasicStroke(2f));
				g2d.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 20, 20);

				g2d.dispose();
			}
		};
		topActions.setOpaque(false);
		topActions.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));

		// Tạo các nút với gradient và hiệu ứng
		playAgainButton = UIEffects.createGradientButton(
				"🔄 Chơi Lại",
				new Color(46, 204, 113),
				new Color(39, 174, 96));
		playAgainButton.setPreferredSize(new Dimension(150, 40));
		UIEffects.addHoverEffect(playAgainButton);

		backToLobbyButton = UIEffects.createGradientButton(
				"🏠 Trở về Sảnh",
				new Color(52, 152, 219),
				new Color(41, 128, 185));
		backToLobbyButton.setPreferredSize(new Dimension(150, 40));
		UIEffects.addHoverEffect(backToLobbyButton);

		// Cancel matchmaking button (hidden until quick play)
		cancelMatchButton = UIEffects.createGradientButton("Hủy ghép trận", new Color(231, 76, 60),
				new Color(192, 57, 43));
		cancelMatchButton.setPreferredSize(new Dimension(150, 40));
		cancelMatchButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
		UIEffects.addHoverEffect(cancelMatchButton);
		cancelMatchButton.setVisible(false);
		cancelMatchButton.addActionListener(e -> {
			if (out != null) {
				out.println("cancel_quick_play");
			}
			isMatchmaking = false;
			hideMatchmakingDialog();
			rootLayout.show(rootPanel, "LOBBY");
		});

		topActions.add(playAgainButton);
		topActions.add(backToLobbyButton);
		// Nút hủy ghép trận ở vùng hành động chính
		topActions.add(cancelMatchButton);
		gameContainer.add(topActions, BorderLayout.NORTH);

		// Khởi tạo không hiển thị
		playAgainButton.setVisible(false);
		backToLobbyButton.setVisible(false);

		playAgainButton.addActionListener(e -> {
			if (offlineMode) {
				restartOfflineGame(); // Chế độ chơi với máy
				playAgainButton.setEnabled(true);
			} else if (out != null) {
				out.println("play_again"); // Chế độ chơi với người
				playAgainButton.setEnabled(false);
				statusLabel.setText("Đã gửi yêu cầu chơi lại, chờ đối thủ...");
			}
		});
		backToLobbyButton.addActionListener(e -> {
			if (out != null)
				out.println("leave");
			rootLayout.show(rootPanel, "LOBBY");
			playAgainButton.setVisible(false);
			backToLobbyButton.setVisible(false);
			gameOver = false;
			clearBoard();
			statusLabel.setText("Đang chờ đối thủ...");
			modeLabel.setText("");
		});

		btnFriend.addActionListener(e -> {
			if (out == null) {
				JOptionPane.showMessageDialog(this, "Chưa kết nối server.");
				return;
			}
			String friend = JOptionPane.showInputDialog(this, "Nhập tên bạn muốn mời:");
			if (friend == null || friend.trim().isEmpty())
				return;
			friend = friend.trim();
			// Tìm trong danh sách online để đảm bảo người đó đang online và free
			boolean found = false;
			boolean isFree = false;
			for (int i = 0; i < onlineModel.size(); i++) {
				String display = onlineModel.get(i);
				String name = parseName(display);
				String status = parseStatus(display);
				if (name.equalsIgnoreCase(friend)) {
					found = true;
					if ("free".equals(status))
						isFree = true;
					break;
				}
			}
			if (!found) {
				JOptionPane.showMessageDialog(this, friend + " không online.");
				return;
			}
			if (!isFree) {
				JOptionPane.showMessageDialog(this, friend + " hiện không sẵn sàng.");
				return;
			}
			// Gửi lời mời
			out.println("challenge:" + friend);
			offlineMode = false; // Đảm bảo đang ở chế độ online
			modeLabel.setText("Chế độ: Chơi với bạn");
			statusLabel.setText("Đang chờ " + friend + " chấp nhận thách đấu...");
			rootLayout.show(rootPanel, "GAME");
		});
		btnRobot.addActionListener(e -> {
			offlineMode = true; // Đảm bảo đang ở chế độ offline
			startOfflineGame();
			modeLabel.setText("Chế độ: Chơi với máy");
			rootLayout.show(rootPanel, "GAME");
		});
		btnQuickPlay.addActionListener(e -> {
			if (out != null) {
				out.println("quick_play");
				offlineMode = false; // Đảm bảo đang ở chế độ online
				modeLabel.setText("Chế độ: Chơi trực tuyến");
				statusLabel.setText("Đang tìm đối thủ...");
				isMatchmaking = true; // Đánh dấu bắt đầu ghép trận
				showMatchmakingDialog();
				// Hiển thị nút hủy ghép trận trên giao diện chính
				if (cancelMatchButton != null)
					cancelMatchButton.setVisible(true);
				rootLayout.show(rootPanel, "GAME");
			} else {
				JOptionPane.showMessageDialog(this, "Chưa kết nối server.");
			}
		});

		rootPanel.add(lobbyPanel, "LOBBY");
		rootPanel.add(gameContainer, "GAME");
		add(rootPanel, BorderLayout.CENTER);

		// Clean shutdown to avoid resource leaks with many windows
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				running = false;
				try {
					if (out != null)
						out.flush();
				} catch (Exception ignore) {
				}
				try {
					if (in != null)
						in.close();
				} catch (Exception ignore) {
				}
				try {
					if (socket != null && !socket.isClosed())
						socket.close();
				} catch (Exception ignore) {
				}
				try {
					if (readerThread != null)
						readerThread.interrupt();
				} catch (Exception ignore) {
				}
			}
		});

		// Không hiển thị UI chính cho tới khi đăng nhập thành công
		connectToServer();
		clearBoard();

		// Handle server messages in a separate thread
		readerThread = new Thread(() -> {
			try {
				while (running && (in != null) && (socket != null) && !socket.isClosed()) {
					String line = in.readLine();
					if (line != null) {
						SwingUtilities.invokeLater(() -> handleServerMessage(line));
					} else {
						SwingUtilities.invokeLater(() -> {
							JOptionPane.showMessageDialog(this, "Mất kết nối với server!");
							offlineMode = true;
							startOfflineGame();
						});
						break;
					}
				}
			} catch (IOException e) {
				if (running) {
					SwingUtilities.invokeLater(() -> {
						JOptionPane.showMessageDialog(this, "Mất kết nối server! Chuyển sang offline.");
						offlineMode = true;
						startOfflineGame();
					});
				}
			}
		}, "client-reader");
		readerThread.setDaemon(true);
		readerThread.start();

		// Sau khi đã sẵn sàng nhận message, hiển thị dialog đăng nhập
		SwingUtilities.invokeLater(this::showLoginDialog);
	}

	private void connectToServer() {
		int attempts = 0;
		final int MAX_ATTEMPTS = 3;
		while (attempts < MAX_ATTEMPTS) {
			try {
				socket = new Socket("localhost", 8001);
				out = new PrintWriter(socket.getOutputStream(), true);
				in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				return;
			} catch (IOException e) {
				attempts++;
				if (attempts == MAX_ATTEMPTS) {
					JOptionPane.showMessageDialog(this,
							"Không thể kết nối server sau " + MAX_ATTEMPTS + " lần thử! Chuyển sang offline.");
					offlineMode = true;
					startOfflineGame();
				} else {
					try {
						Thread.sleep(2000); // Chờ 2 giây trước khi thử lại
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
					}
				}
			}
		}
	}

	private void handleServerMessage(String msg) {
		if (msg == null)
			return;
		if (msg.startsWith("auth_ok:")) {
			String u = msg.substring(8);
			this.myName = u;
			saveCredentials(u, getPendingPassword());
			setTitle("Cờ Caro 3x3 - " + u);
			JOptionPane.showMessageDialog(this, "Đăng nhập thành công: " + u);
			// Hiển thị UI chính sau khi xác thực thành công
			if (!isVisible())
				setVisible(true);
			// Update avatar/name display if present
			if (userNameLabel != null)
				userNameLabel.setText(u);
			if (userAvatarLabel != null)
				userAvatarLabel.setIcon(makeAvatarIcon(u, 40));
			rootLayout.show(rootPanel, "LOBBY");
			return;

		} else if (msg.equals("auth_fail")) {
			JOptionPane.showMessageDialog(this, "Sai tài khoản hoặc mật khẩu");
			showLoginDialog();
			return;
		} else if (msg.equals("register_ok")) {
			JOptionPane.showMessageDialog(this, "Đăng ký thành công. Vui lòng đăng nhập!");
			showLoginDialog();
			return;
		} else if (msg.equals("register_dup")) {
			JOptionPane.showMessageDialog(this, "Tên tài khoản đã tồn tại. Vui lòng chọn tên khác.");
			showRegisterDialog();
			return;
		}
		if (msg.startsWith("stats:")) {
			String payload = msg.substring(6);
			String[] parts = payload.split(";");
			String wins = parts.length > 0 && parts[0].contains("=") ? parts[0].split("=")[1] : "0";
			String losses = parts.length > 1 && parts[1].contains("=") ? parts[1].split("=")[1] : "0";
			String draws = parts.length > 2 && parts[2].contains("=") ? parts[2].split("=")[1] : "0";
			String rate = parts.length > 3 && parts[3].contains("=") ? parts[3].split("=")[1] : "0%";
			showLeaderboard(wins, losses, draws, rate);
		} else if (msg.startsWith("top:")) {
			String payload = msg.substring(4);
			showTopLeaderboard(payload);
		} else if (msg.startsWith("online:")) {
			String payload = msg.substring(7);
			String[] items = payload.isEmpty() ? new String[0] : payload.split(",");
			onlineModel.clear();
			for (String it : items) {
				String[] pair = it.split("\\|", 2);
				String name = pair.length > 0 ? pair[0] : it;
				String status = pair.length > 1 ? pair[1] : "free";
				if (!name.equals(myName)) {
					String display = name + " (" + status + ")";
					onlineModel.addElement(display);
				}
			}
		} else if (msg.startsWith("invite:")) {
			String challenger = msg.substring(7);
			int option = JOptionPane.showConfirmDialog(this, challenger + " muốn thách đấu bạn. Chấp nhận?",
					"Thách đấu", JOptionPane.YES_NO_OPTION);
			if (option == JOptionPane.YES_OPTION && out != null) {
				out.println("accept:" + challenger);
				rootLayout.show(rootPanel, "GAME");
				statusLabel.setText("Trò chơi bắt đầu! Bạn là O, chờ lượt...");
				myMark = 'O';
				myTurn = false;
				modeLabel.setText("Chế độ: Chơi với bạn");
			} else if (out != null) {
				out.println("reject:" + challenger);
				// Khi từ chối thách đấu, quay về sảnh chính và cập nhật trạng thái
				rootLayout.show(rootPanel, "LOBBY");
				statusLabel.setText("Bạn đã từ chối thách đấu.");
			}
		} else if (msg.startsWith("rejected:")) {
			String rejector = msg.substring(9);
			JOptionPane.showMessageDialog(this, rejector + " đã từ chối thách đấu.");
			// Quay về sảnh chính khi lời mời bị từ chối
			rootLayout.show(rootPanel, "LOBBY");
			statusLabel.setText(rejector + " đã từ chối thách đấu.");
		} else if (msg.startsWith("start:")) {
			String mark = msg.substring(6);
			myMark = mark.charAt(0);
			isMatchmaking = false; // Kết thúc trạng thái ghép trận
			isInGame = true; // Bắt đầu trạng thái trong game
			hideMatchmakingDialog(); // Ẩn dialog matchmaking khi bắt đầu trận
			rootLayout.show(rootPanel, "GAME");
			clearBoard();
			gameOver = false;
			if (myMark == 'X') {
				myTurn = true;
				statusLabel.setText("Trò chơi bắt đầu! Bạn là X, lượt của bạn.");
			} else {
				myTurn = false;
				statusLabel.setText("Trò chơi bắt đầu! Bạn là O, chờ lượt...");
			}
			// Update mode label depending on whether offlineMode is set
			if (offlineMode) {
				modeLabel.setText("Chế độ: Chơi với máy");
			} else {
				modeLabel.setText("Chế độ: Chơi trực tuyến");
			}
			updateButtonsEnabled();
		} else if (msg.startsWith("move:")) {
			hideMatchmakingDialog(); // Ẩn dialog matchmaking khi nhận được nước đi
			String[] parts = msg.substring(5).split(",");
			int row = Integer.parseInt(parts[0]);
			int col = Integer.parseInt(parts[1]);
			char opponentMark = (myMark == 'X') ? 'O' : 'X';
			board[row][col] = opponentMark;
			buttons[row][col].setText(String.valueOf(opponentMark));
			buttons[row][col].setIcon(null);
			buttons[row][col].setFont(new Font("Arial", Font.BOLD, 64));
			buttons[row][col].setForeground(Color.WHITE);
			if (checkWin(opponentMark)) {
				statusLabel.setText("Đối thủ thắng! Bạn thua.");
				gameOver = true;
				isInGame = false; // Kết thúc trạng thái trong game
				disableButtons();
				playAgainButton.setVisible(true);
				backToLobbyButton.setVisible(true);
			} else if (isDraw()) {
				statusLabel.setText("Hòa!");
				gameOver = true;
				isInGame = false; // Kết thúc trạng thái trong game
				disableButtons();
				playAgainButton.setVisible(true);
				backToLobbyButton.setVisible(true);
			} else {
				myTurn = true;
				statusLabel.setText("Lượt của bạn!");
			}
			updateButtonsEnabled();
		} else if (msg.startsWith("opponent_wants_rematch")) {
			int option = JOptionPane.showConfirmDialog(this, "Đối thủ muốn chơi lại. Chấp nhận?", "Chơi lại",
					JOptionPane.YES_NO_OPTION);
			if (option == JOptionPane.YES_OPTION && out != null) {
				out.println("play_again");
			}
		} else if (msg.startsWith("restart")) {
			clearBoard();
			gameOver = false;
			playAgainButton.setVisible(false);
			playAgainButton.setEnabled(true);
			backToLobbyButton.setVisible(false);
			if (myMark == 'X') {
				myTurn = true;
				statusLabel.setText("Trò chơi bắt đầu lại! Lượt của bạn.");
			} else {
				myTurn = false;
				statusLabel.setText("Trò chơi bắt đầu lại! Chờ lượt...");
			}
			updateButtonsEnabled();
		} else if (msg.equals("your_turn")) {
			myTurn = true;
			statusLabel.setText("Lượt của bạn!");
			updateButtonsEnabled();
		} else if (msg.equals("wait")) {
			// 'wait' can mean two things depending on context:
			// - during matchmaking: server tells client to wait for a match
			// - during a game: server tells client to wait for opponent's move
			myTurn = false;
			if (isMatchmaking) {
				statusLabel.setText("Đang ghép trận... Vui lòng đợi");
				if (matchmakingDialog == null || !matchmakingDialog.isVisible()) {
					showMatchmakingDialog();
				}
			} else {
				// Normal in-game wait state
				statusLabel.setText("Đang chờ đối thủ...");
				// ensure matchmaking dialog is not shown
				hideMatchmakingDialog();
			}
			updateButtonsEnabled();
		} else if (msg.equals("cancelled")) {
			hideMatchmakingDialog();
			statusLabel.setText("Đã hủy ghép trận");
		} else if (msg.equals("win")) {
			statusLabel.setText("Bạn thắng!");
			gameOver = true;
			disableButtons();
			playAgainButton.setVisible(true);
			backToLobbyButton.setVisible(true);
		} else if (msg.equals("lose")) {
			statusLabel.setText("Đối thủ thắng! Bạn thua.");
			gameOver = true;
			disableButtons();
			playAgainButton.setVisible(true);
			backToLobbyButton.setVisible(true);
		} else if (msg.equals("draw")) {
			statusLabel.setText("Hòa!");
			gameOver = true;
			disableButtons();
			playAgainButton.setVisible(true);
			backToLobbyButton.setVisible(true);
		} else if (msg.startsWith("opponent_left:")) {
			String leftName = msg.substring(14);
			JOptionPane.showMessageDialog(this, leftName + " đã rời đi.");
			rootLayout.show(rootPanel, "LOBBY");
			gameOver = false;
			clearBoard();
		} else if (msg.startsWith("error:")) {
			String errorMsg = msg.substring(6);
			JOptionPane.showMessageDialog(this, errorMsg, "Lỗi", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void makeMove(int row, int col) {
		if (gameOver || !myTurn || board[row][col] != ' ')
			return;

		hideMatchmakingDialog(); // Ẩn dialog ghép trận khi đánh

		// Đánh dấu nước đi
		board[row][col] = myMark;
		buttons[row][col].setText(String.valueOf(myMark));
		buttons[row][col].setIcon(null);
		buttons[row][col].setFont(new Font("Arial", Font.BOLD, 64));
		buttons[row][col].setForeground(Color.WHITE);

		if (offlineMode) {
			// Xử lý nước đi trong chế độ chơi với máy
			if (checkWin(myMark)) {
				statusLabel.setText("Bạn thắng máy! Xuất sắc!");
				gameOver = true;
				disableButtons();
				playAgainButton.setVisible(true);
				backToLobbyButton.setVisible(true);
			} else if (isDraw()) {
				statusLabel.setText("Hòa với máy! Khá đấy!");
				gameOver = true;
				disableButtons();
				playAgainButton.setVisible(true);
				backToLobbyButton.setVisible(true);
			} else {
				myTurn = false;
				statusLabel.setText("Máy đang suy nghĩ...");
				updateButtonsEnabled();
				if (aiThinking)
					return;
				aiThinking = true;
				new Thread(() -> {
					try {
						Thread.sleep(500); // Delay để mô phỏng suy nghĩ
						SwingUtilities.invokeLater(() -> {
							Point aiMove = pickAiMove();
							if (aiMove != null) {
								char aiMark = (myMark == 'X') ? 'O' : 'X';
								board[aiMove.x][aiMove.y] = aiMark;
								buttons[aiMove.x][aiMove.y].setText(String.valueOf(aiMark));
								buttons[aiMove.x][aiMove.y].setIcon(null);
								buttons[aiMove.x][aiMove.y].setFont(new Font("Arial", Font.BOLD, 64));
								buttons[aiMove.x][aiMove.y].setForeground(Color.WHITE);

								if (checkWin(aiMark)) {
									statusLabel.setText("Máy thắng! Thử lại nhé!");
									gameOver = true;
									disableButtons();
									playAgainButton.setVisible(true);
									backToLobbyButton.setVisible(true);
								} else if (isDraw()) {
									statusLabel.setText("Hòa với máy! Khá đấy!");
									gameOver = true;
									disableButtons();
									playAgainButton.setVisible(true);
									backToLobbyButton.setVisible(true);
								} else {
									myTurn = true;
									statusLabel.setText("Lượt của bạn!");
								}
							}
							updateButtonsEnabled();
							aiThinking = false;
						});
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						aiThinking = false;
					}
				}).start();
			}
		} else {
			// Xử lý nước đi trong chế độ chơi trực tuyến
			if (!offlineMode && out != null) {
				out.println("move:" + row + "," + col);
				if (checkWin(myMark)) {
					statusLabel.setText("Bạn thắng!");
					gameOver = true;
					disableButtons();
					playAgainButton.setVisible(true);
					backToLobbyButton.setVisible(true);
				} else if (isDraw()) {
					statusLabel.setText("Hòa!");
					gameOver = true;
					disableButtons();
					playAgainButton.setVisible(true);
					backToLobbyButton.setVisible(true);
				} else {
					myTurn = false;
					statusLabel.setText("Đợi đối thủ...");
				}
			}
		}
		updateButtonsEnabled();
	}

	private void updateButtonsEnabled() {
		for (int i = 0; i < 3; i++)
			for (int j = 0; j < 3; j++)
				buttons[i][j].setEnabled(!gameOver && board[i][j] == ' ' && myTurn);
	}

	private void disableButtons() {
		for (int i = 0; i < 3; i++)
			for (int j = 0; j < 3; j++)
				buttons[i][j].setEnabled(false);
	}

	private void clearBoard() {
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				board[i][j] = ' ';
				buttons[i][j].setIcon(null);
				buttons[i][j].setText(" ");
				buttons[i][j].setEnabled(true); // Bật lại các nút
				buttons[i][j].setBackground(new Color(22, 27, 34));
				buttons[i][j].setFont(new Font("Arial", Font.BOLD, 48));
			}
		}
		// Reset các biến trạng thái
		gameOver = false;
		myTurn = true;
		aiThinking = false;
	}

	private void startOfflineGame() {
		offlineMode = true;
		clearBoard();
		myMark = 'X'; // Người chơi là 'X', máy là 'O'
		myTurn = true;
		gameOver = false;
		statusLabel.setText("Lượt của bạn! (Bạn là X, máy là O)");
		playAgainButton.setVisible(false);
		backToLobbyButton.setVisible(false);
		rootLayout.show(rootPanel, "GAME");
		aiThinking = false;
		updateButtonsEnabled();
	}

	private void restartOfflineGame() {
		// Đặt lại trạng thái ban đầu
		clearBoard();
		// Reset giao diện
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				buttons[i][j].setEnabled(true);
				buttons[i][j].setIcon(null);
				buttons[i][j].setText(" ");
			}
		}
		// Reset các biến trạng thái game
		myMark = 'X';
		myTurn = true;
		gameOver = false;
		aiThinking = false;

		// Reset giao diện điều khiển
		statusLabel.setText("Lượt của bạn! (Bạn là X, máy là O)");
		playAgainButton.setVisible(false);
		backToLobbyButton.setVisible(false);

		// Cập nhật trạng thái các nút
		updateButtonsEnabled();

		// Đảm bảo board được reset
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				board[i][j] = ' ';
			}
		}
	}

	private String parseName(String display) {
		int idx = display.indexOf(" (");
		return idx > 0 ? display.substring(0, idx) : display;
	}

	private String parseStatus(String display) {
		return display.contains("(free)") ? "free" : "busy";
	}

	private boolean checkWin(char mark) {
		for (int i = 0; i < 3; i++) {
			if (board[i][0] == mark && board[i][1] == mark && board[i][2] == mark)
				return true;
			if (board[0][i] == mark && board[1][i] == mark && board[2][i] == mark)
				return true;
		}
		if (board[0][0] == mark && board[1][1] == mark && board[2][2] == mark)
			return true;
		if (board[0][2] == mark && board[1][1] == mark && board[2][0] == mark)
			return true;
		return false;
	}

	private boolean isDraw() {
		for (int i = 0; i < 3; i++)
			for (int j = 0; j < 3; j++)
				if (board[i][j] == ' ')
					return false;
		return true;
	}

	private Point pickAiMove() {
		char aiMark = (myMark == 'X') ? 'O' : 'X';
		char opponentMark = myMark;

		// Kiểm tra nếu AI có thể thắng ngay
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				if (board[i][j] == ' ') {
					board[i][j] = aiMark;
					if (checkWin(aiMark)) {
						board[i][j] = ' ';
						return new Point(i, j);
					}
					board[i][j] = ' ';
				}
			}
		}

		// Kiểm tra và ngăn người chơi thắng
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				if (board[i][j] == ' ') {
					board[i][j] = opponentMark;
					if (checkWin(opponentMark)) {
						board[i][j] = ' ';
						return new Point(i, j);
					}
					board[i][j] = ' ';
				}
			}
		}

		// Chọn vị trí trung tâm nếu trống
		if (board[1][1] == ' ') {
			return new Point(1, 1);
		}

		// Chọn các góc nếu có
		int[][] corners = { { 0, 0 }, { 0, 2 }, { 2, 0 }, { 2, 2 } };
		Random rand = new Random();
		for (int attempt = 0; attempt < corners.length; attempt++) {
			int idx = rand.nextInt(corners.length);
			int[] corner = corners[idx];
			if (board[corner[0]][corner[1]] == ' ') {
				return new Point(corner[0], corner[1]);
			}
		}

		// Chọn ngẫu nhiên ô còn lại nếu không có góc
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				if (board[i][j] == ' ') {
					return new Point(i, j);
				}
			}
		}
		return null; // Bàn cờ đầy, không có nước đi
	}

	private String printBoard() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				sb.append(board[i][j]).append(" ");
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	private JButton createStyledButton(String text, Color color) {
		JButton button = new JButton(text);
		button.setFont(new Font("Arial", Font.BOLD, 14));
		button.setBackground(color);
		button.setForeground(Color.WHITE);
		button.setFocusPainted(false);
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

	/**
	 * Create a JLabel containing the login/register banner image.
	 * Tries several common locations (classpath resource and project asset paths).
	 * If no image found, returns an empty JLabel.
	 */
	private JLabel createBannerLabel() {
		String[] candidates = new String[] {
				"docs/tictactoe.png", // docs folder (user provided)
				"/assets/login_banner.png", // classpath resource
				"assets/login_banner.png", // project root assets folder
				"src/Client/images/login_banner.png", // dev source path
				"src/Client/resources/login_banner.png"
		};
		ImageIcon icon = null;
		for (String p : candidates) {
			// try classpath
			java.net.URL url = getClass().getResource(p);
			if (url != null) {
				icon = new ImageIcon(url);
				break;
			}
			// try file path
			java.io.File f = new java.io.File(p);
			if (f.exists()) {
				icon = new ImageIcon(f.getAbsolutePath());
				break;
			}
		}
		if (icon == null)
			return new JLabel();
		// scale image to fit banner area while keeping aspect ratio
		int targetW = 200;
		int targetH = 200;
		Image img = icon.getImage();
		Image scaled = img.getScaledInstance(targetW, targetH, Image.SCALE_SMOOTH);
		JLabel lbl = new JLabel(new ImageIcon(scaled));
		lbl.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		lbl.setHorizontalAlignment(JLabel.CENTER);
		lbl.setOpaque(false);
		return lbl;
	}

	/**
	 * Load the background image from known candidate locations. Returns null if not
	 * found.
	 */
	private Image loadBackgroundImage() {
		String[] candidates = new String[] {
				"docs/tictactoe.png",
				"/assets/login_banner.png",
				"assets/login_banner.png",
				"src/Client/images/login_banner.png",
				"src/Client/resources/login_banner.png"
		};
		for (String p : candidates) {
			java.net.URL url = getClass().getResource(p);
			if (url != null) {
				ImageIcon ic = new ImageIcon(url);
				return ic.getImage();
			}
			java.io.File f = new java.io.File(p);
			if (f.exists()) {
				ImageIcon ic = new ImageIcon(f.getAbsolutePath());
				return ic.getImage();
			}
		}
		return null;
	}

	/**
	 * Create a simple circular avatar icon with initials.
	 */
	private ImageIcon makeAvatarIcon(String name, int size) {
		String initials = "?";
		if (name != null && !name.trim().isEmpty()) {
			String[] parts = name.trim().split("\\s+");
			if (parts.length == 1)
				initials = parts[0].substring(0, 1).toUpperCase();
			else
				initials = (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
		}
		int s = Math.max(24, size);
		BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		int h = Math.abs((name == null ? 0 : name.hashCode()));
		Color bg = new Color((h >> 16) & 0xFF, (h >> 8) & 0xFF, h & 0xFF);
		// ensure not too dark
		bg = bg.brighter();
		g.setColor(bg);
		g.fillOval(0, 0, s, s);
		g.setColor(Color.WHITE);
		Font f = new Font("Segoe UI", Font.BOLD, s / 2);
		g.setFont(f);
		FontMetrics fm = g.getFontMetrics();
		int tx = (s - fm.stringWidth(initials)) / 2;
		int ty = (s - fm.getHeight()) / 2 + fm.getAscent();
		g.drawString(initials, tx, ty);
		g.dispose();
		return new ImageIcon(img);
	}

	/**
	 * Panel that paints a scaled background image.
	 */
	private static class RoundedButton extends JButton {
		public RoundedButton(String text) {
			super(text);
			setContentAreaFilled(false);
			setFocusPainted(false);
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			if (getModel().isPressed()) {
				g2.setColor(getBackground().darker());
			} else if (getModel().isRollover()) {
				g2.setColor(getBackground().brighter());
			} else {
				g2.setColor(getBackground());
			}
			g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
			super.paintComponent(g);
			g2.dispose();
		}
	}

	private static class RoundedBorder extends AbstractBorder {
		private Color color;
		private int radius;

		RoundedBorder(Color color, int radius) {
			this.color = color;
			this.radius = radius;
		}

		@Override
		public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
			Graphics2D g2d = (Graphics2D) g.create();
			g2d.setColor(color);
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2d.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
			g2d.dispose();
		}

		@Override
		public Insets getBorderInsets(Component c) {
			return new Insets(radius / 2, radius / 2, radius / 2, radius / 2);
		}

		@Override
		public boolean isBorderOpaque() {
			return false;
		}
	}

	private static class BackgroundPanel extends JPanel {
		private Image bg;

		BackgroundPanel(Image img) {
			this.bg = img;
			setLayout(new BorderLayout());
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			if (bg != null) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				g2.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
				g2.dispose();
			}
		}
	}

	/**
	 * Create a translucent rounded "glass" panel for forms placed over the
	 * background.
	 */
	private JPanel createGlassPanel(int width) {
		JPanel glass = new JPanel() {
			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				int arc = 20;
				Color c = new Color(255, 255, 255, 230); // translucent white
				g2.setColor(c);
				g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
				g2.setColor(new Color(0, 0, 0, 30));
				g2.setStroke(new BasicStroke(1f));
				g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
				g2.dispose();
				super.paintComponent(g);
			}
		};
		glass.setOpaque(false);
		glass.setLayout(new BoxLayout(glass, BoxLayout.Y_AXIS));
		glass.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
		if (width > 0)
			glass.setPreferredSize(new Dimension(width, 1));
		return glass;
	}

	// === Auth UI & local persistence ===

	private JPanel createFormField(String placeholder, String icon) {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setOpaque(false);
		panel.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.setMaximumSize(new Dimension(340, 85));

		// Field label with icon
		JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
		labelPanel.setOpaque(false);

		JLabel label = new JLabel(placeholder);
		label.setForeground(new Color(255, 255, 255));
		label.setFont(new Font("Arial", Font.BOLD, 14));
		labelPanel.add(label);

		panel.add(labelPanel);
		panel.add(Box.createVerticalStrut(5));

		// Field wrapper panel
		JPanel fieldWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		fieldWrapper.setOpaque(false);
		fieldWrapper.setMaximumSize(new Dimension(320, 45));
		panel.add(fieldWrapper);

		return panel;
	}

	// ================= ĐĂNG NHẬP ==================
	private void showLoginDialog() {
		JDialog dialog = new JDialog(this, "Đăng nhập", true);
		dialog.setSize(400, 650); // Tăng kích thước dialog
		dialog.setLocationRelativeTo(this);

		// Create main panel with improved background
		JPanel mainPanel = new JPanel() {
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				Graphics2D g2d = (Graphics2D) g;
				g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				GradientPaint gp = new GradientPaint(0, 0, new Color(0, 162, 232),
						getWidth(), getHeight(), new Color(0, 210, 255));
				g2d.setPaint(gp);
				g2d.fillRect(0, 0, getWidth(), getHeight());
			}
		};
		mainPanel.setLayout(new BorderLayout(10, 10));
		mainPanel.setBorder(new EmptyBorder(20, 30, 20, 30));

		dialog.setContentPane(mainPanel);

		// Main content panel
		JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setOpaque(false);

		// Logo panel
		JPanel logoPanel = new JPanel(new BorderLayout());
		logoPanel.setOpaque(false);
		logoPanel.setPreferredSize(new Dimension(200, 200));
		logoPanel.setBorder(new EmptyBorder(20, 0, 40, 0));

		// Load and display logo image
		try {
			File logoFile = new File("docs/tictactoe.png");
			if (logoFile.exists()) {
				ImageIcon icon = new ImageIcon(logoFile.getAbsolutePath());
				Image img = icon.getImage();
				Image scaledImg = img.getScaledInstance(180, 180, Image.SCALE_SMOOTH);
				JLabel logoLabel = new JLabel(new ImageIcon(scaledImg));
				logoLabel.setHorizontalAlignment(JLabel.CENTER);
				logoPanel.add(logoLabel, BorderLayout.CENTER);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		content.add(logoPanel);

		// Form fields with rounded corners
		JPanel formPanel = new JPanel() {
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				Graphics2D g2d = (Graphics2D) g.create();
				g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2d.setColor(new Color(255, 255, 255, 30));
				g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
				g2d.dispose();
			}
		};
		formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
		formPanel.setOpaque(false);
		formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

		// Username field
		JPanel usernamePanel = createFormField("Tài khoản", "\uf007");
		JTextField userField = new JTextField(20);
		userField.setPreferredSize(new Dimension(320, 45));
		userField.setBorder(BorderFactory.createCompoundBorder(
				new RoundedBorder(new Color(230, 230, 230), 15),
				BorderFactory.createEmptyBorder(8, 12, 8, 12)));
		userField.setFont(new Font("Arial", Font.PLAIN, 14));
		userField.setForeground(new Color(60, 60, 60));
		userField.setBackground(Color.WHITE);
		userField.setCaretColor(new Color(60, 60, 60));
		userField.setBackground(Color.WHITE);
		usernamePanel.add(userField);
		formPanel.add(usernamePanel);
		formPanel.add(Box.createVerticalStrut(20));

		// Password field
		JPanel passwordPanel = createFormField("Mật khẩu", "\uf023");
		JPasswordField passField = new JPasswordField(20);
		passField.setPreferredSize(new Dimension(320, 45));
		passField.setBorder(BorderFactory.createCompoundBorder(
				new RoundedBorder(new Color(230, 230, 230), 15),
				BorderFactory.createEmptyBorder(8, 12, 8, 12)));
		passField.setFont(new Font("Arial", Font.PLAIN, 14));
		passField.setForeground(new Color(60, 60, 60));
		passField.setBackground(Color.WHITE);
		passField.setCaretColor(new Color(60, 60, 60));
		passField.setBackground(Color.WHITE);
		passwordPanel.add(passField);
		formPanel.add(passwordPanel);
		formPanel.add(Box.createVerticalStrut(30));

		// Forgot password link
		JButton forgotPasswordBtn = new JButton("Quên mật khẩu?");
		forgotPasswordBtn.setForeground(new Color(255, 255, 200));
		forgotPasswordBtn.setBorderPainted(false);
		forgotPasswordBtn.setContentAreaFilled(false);
		forgotPasswordBtn.setFont(new Font("Arial", Font.PLAIN, 14));
		forgotPasswordBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
		forgotPasswordBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		formPanel.add(forgotPasswordBtn);
		formPanel.add(Box.createVerticalStrut(20));

		// Login button
		RoundedButton btnLogin = new RoundedButton("ĐĂNG NHẬP");
		btnLogin.setFont(new Font("Arial", Font.BOLD, 16));
		btnLogin.setForeground(new Color(50, 50, 50));
		btnLogin.setBackground(new Color(255, 255, 0));
		btnLogin.setBorder(new EmptyBorder(12, 40, 12, 40));
		btnLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));
		btnLogin.setFocusPainted(false);
		btnLogin.setAlignmentX(Component.CENTER_ALIGNMENT);
		formPanel.add(btnLogin);
		formPanel.add(Box.createVerticalStrut(20));

		// Register link
		JPanel linkPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		linkPanel.setOpaque(false);
		JLabel registerLabel = new JLabel("Chưa có tài khoản? ");
		registerLabel.setForeground(Color.WHITE);
		JButton btnToRegister = new JButton("Đăng ký");
		btnToRegister.setForeground(Color.YELLOW);
		btnToRegister.setBorderPainted(false);
		btnToRegister.setContentAreaFilled(false);
		btnToRegister.setCursor(new Cursor(Cursor.HAND_CURSOR));
		linkPanel.add(registerLabel);
		linkPanel.add(btnToRegister);
		formPanel.add(linkPanel);

		content.add(formPanel);

		// Error label
		JLabel errorLabel = new JLabel("", JLabel.CENTER);
		errorLabel.setForeground(new Color(255, 100, 100));
		errorLabel.setFont(new Font("Arial", Font.PLAIN, 12));
		errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		content.add(errorLabel);

		mainPanel.add(content, BorderLayout.CENTER);

		// Action for forgot password
		forgotPasswordBtn.addActionListener(e -> {
			String username = userField.getText().trim();
			if (username.isEmpty()) {
				errorLabel.setText("⚠️ Vui lòng nhập tên tài khoản!");
				return;
			}
			if (out == null)
				connectToServer();
			if (out != null) {
				out.println("forgot:" + username);
				JOptionPane.showMessageDialog(dialog,
						"Yêu cầu đặt lại mật khẩu đã được gửi.\nVui lòng kiểm tra email của bạn.",
						"Quên mật khẩu",
						JOptionPane.INFORMATION_MESSAGE);
			}
		});

		// Action for register
		btnToRegister.addActionListener(e -> {
			dialog.dispose();
			showRegisterDialog();
		});

		btnLogin.addActionListener(e -> {
			String u = userField.getText().trim();
			String p = new String(passField.getPassword());

			if (u.isEmpty() || p.isEmpty()) {
				errorLabel.setText("⚠️ Vui lòng nhập đầy đủ tài khoản và mật khẩu!");
				return;
			}

			if (out == null)
				connectToServer();
			if (out != null) {
				pendingPassword = p;
				out.println("auth:" + u + ":" + p);
				saveCredentials(u, p);
			}
			// Không thông báo thành công, chỉ hiển thị lỗi nếu sai
			dialog.dispose();
		});

		// set composed panel as dialog content so background is visible
		dialog.setContentPane(mainPanel);
		dialog.setVisible(true);
	}

	// ================= ĐĂNG KÝ ==================
	private void showRegisterDialog() {
		JDialog dialog = new JDialog(this, "Đăng ký", true);
		dialog.setSize(380, 600);
		dialog.setLocationRelativeTo(this);

		// Create main panel with cyan background
		JPanel mainPanel = new JPanel() {
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				Graphics2D g2d = (Graphics2D) g;
				g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				GradientPaint gp = new GradientPaint(0, 0, new Color(0, 180, 255),
						getWidth(), getHeight(), new Color(0, 210, 255));
				g2d.setPaint(gp);
				g2d.fillRect(0, 0, getWidth(), getHeight());
			}
		};
		mainPanel.setLayout(new BorderLayout(10, 10));
		mainPanel.setBorder(new EmptyBorder(20, 30, 20, 30));
		dialog.setContentPane(mainPanel);

		// Main content panel
		JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setOpaque(false);

		// Back button at top left
		JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		topPanel.setOpaque(false);
		JButton backButton = new JButton("←");
		backButton.setFont(new Font("Segoe UI", Font.BOLD, 20));
		backButton.setForeground(Color.WHITE);
		backButton.setBorderPainted(false);
		backButton.setContentAreaFilled(false);
		backButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
		topPanel.add(backButton);
		content.add(topPanel);

		// Logo panel
		JPanel logoPanel = new JPanel(new BorderLayout());
		logoPanel.setOpaque(false);
		logoPanel.setPreferredSize(new Dimension(200, 200));
		logoPanel.setBorder(new EmptyBorder(10, 0, 20, 0));

		// Load and display logo image
		try {
			File logoFile = new File("docs/tictactoe.png");
			if (logoFile.exists()) {
				ImageIcon icon = new ImageIcon(logoFile.getAbsolutePath());
				Image img = icon.getImage();
				Image scaledImg = img.getScaledInstance(180, 180, Image.SCALE_SMOOTH);
				JLabel logoLabel = new JLabel(new ImageIcon(scaledImg));
				logoLabel.setHorizontalAlignment(JLabel.CENTER);
				logoPanel.add(logoLabel, BorderLayout.CENTER);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		content.add(logoPanel);

		// Title
		JLabel title = new JLabel("ĐĂNG KÝ", JLabel.CENTER);
		title.setFont(new Font("Arial", Font.BOLD, 24));
		title.setForeground(Color.WHITE);
		title.setAlignmentX(Component.CENTER_ALIGNMENT);
		content.add(title);
		content.add(Box.createVerticalStrut(20));

		// Username field
		JPanel userPanel = createFormField("Tài khoản", "\uf007");
		JTextField userField = new JTextField(20);
		userField.setPreferredSize(new Dimension(280, 35));
		userField.setBorder(BorderFactory.createCompoundBorder(
				new LineBorder(Color.WHITE, 1, true),
				BorderFactory.createEmptyBorder(5, 10, 5, 10)));
		userField.setBackground(Color.WHITE);
		userPanel.add(userField);
		content.add(userPanel);
		content.add(Box.createVerticalStrut(15));

		// Password field
		JPanel passPanel = createFormField("Mật khẩu", "\uf023");
		JPasswordField passField = new JPasswordField(20);
		passField.setPreferredSize(new Dimension(280, 35));
		passField.setBorder(BorderFactory.createCompoundBorder(
				new LineBorder(Color.WHITE, 1, true),
				BorderFactory.createEmptyBorder(5, 10, 5, 10)));
		passField.setBackground(Color.WHITE);
		passPanel.add(passField);
		content.add(passPanel);
		content.add(Box.createVerticalStrut(15));

		// Confirm password field
		JPanel confirmPanel = createFormField("Xác nhận mật khẩu", "\uf023");
		JPasswordField confirmField = new JPasswordField(20);
		confirmField.setPreferredSize(new Dimension(280, 35));
		confirmField.setBorder(BorderFactory.createCompoundBorder(
				new LineBorder(Color.WHITE, 1, true),
				BorderFactory.createEmptyBorder(5, 10, 5, 10)));
		confirmField.setBackground(Color.WHITE);
		confirmPanel.add(confirmField);
		content.add(confirmPanel);
		content.add(Box.createVerticalStrut(30));

		// Register button
		JButton btnRegister = new JButton("ĐĂNG KÝ");
		btnRegister.setFont(new Font("Arial", Font.BOLD, 16));
		btnRegister.setForeground(new Color(50, 50, 50));
		btnRegister.setBackground(new Color(255, 255, 0));
		btnRegister.setBorder(new EmptyBorder(12, 40, 12, 40));
		btnRegister.setFocusPainted(false);
		btnRegister.setCursor(new Cursor(Cursor.HAND_CURSOR));
		btnRegister.setAlignmentX(Component.CENTER_ALIGNMENT);
		content.add(btnRegister);
		content.add(Box.createVerticalStrut(20));

		// Error label
		JLabel errorLabel = new JLabel("");
		errorLabel.setForeground(new Color(255, 100, 100));
		errorLabel.setFont(new Font("Arial", Font.PLAIN, 12));
		errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		content.add(errorLabel);
		content.add(Box.createVerticalStrut(10));

		// Login link
		JPanel linkPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		linkPanel.setOpaque(false);
		JLabel loginLabel = new JLabel("Đã có tài khoản? ");
		loginLabel.setForeground(new Color(255, 255, 200));
		loginLabel.setFont(new Font("Arial", Font.PLAIN, 14));
		JButton btnToLogin = new JButton("Đăng nhập");
		btnToLogin.setForeground(new Color(255, 255, 0));
		btnToLogin.setFont(new Font("Arial", Font.BOLD, 14));
		btnToLogin.setBorderPainted(false);
		btnToLogin.setContentAreaFilled(false);
		btnToLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));
		linkPanel.add(loginLabel);
		linkPanel.add(btnToLogin);
		content.add(linkPanel);

		// Error label for registration
		JLabel registerErrorLabel = new JLabel("", JLabel.CENTER);
		registerErrorLabel.setForeground(new Color(255, 100, 100));
		registerErrorLabel.setFont(new Font("Arial", Font.PLAIN, 12));
		registerErrorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		content.add(registerErrorLabel);

		mainPanel.add(content, BorderLayout.CENTER);

		// Actions
		backButton.addActionListener(e -> {
			dialog.dispose();
			showLoginDialog();
		});

		btnToLogin.addActionListener(e -> {
			dialog.dispose();
			showLoginDialog();
		});

		btnRegister.addActionListener(e -> {
			String u = userField.getText().trim();
			String p = new String(passField.getPassword());
			String c = new String(confirmField.getPassword());

			if (u.isEmpty() || p.isEmpty() || c.isEmpty()) {
				registerErrorLabel.setText("⚠️ Vui lòng điền đầy đủ thông tin!");
				return;
			}

			if (u.length() < 3) {
				registerErrorLabel.setText("⚠️ Tên tài khoản phải có ít nhất 3 ký tự!");
				return;
			}

			if (p.length() < 6) {
				registerErrorLabel.setText("⚠️ Mật khẩu phải có ít nhất 6 ký tự!");
				return;
			}

			if (!p.equals(c)) {
				registerErrorLabel.setText("⚠️ Mật khẩu xác nhận không khớp!");
				return;
			}

			registerErrorLabel.setText("");
			if (out == null) {
				connectToServer();
			}

			if (out != null) {
				dialog.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				btnRegister.setEnabled(false);
				out.println("register:" + u + ":" + p);
				dialog.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				errorLabel.setText("Đang đăng ký...");
				btnRegister.setEnabled(false);
			} else {
				errorLabel.setText("⚠️ Không thể kết nối đến máy chủ!");
				return;
			}

			if (!p.equals(c)) {
				errorLabel.setText("⚠️ Mật khẩu nhập lại không khớp!");
				return;
			}

			if (out == null)
				connectToServer();
			if (out != null) {
				out.println("register:" + u + ":" + p);
				dialog.dispose();
			}
		});

		dialog.setVisible(true);

		dialog.setVisible(true);
	}

	// HÀM STYLE CHUNG CHO NÚT
	private void saveCredentials(String user, String pass) {
		try {
			File dir = new File(System.getProperty("user.home"), ".caro3x3");
			dir.mkdirs();
			try (FileWriter fw = new FileWriter(new File(dir, "creds.txt"))) {
				fw.write(user + "\n");
				fw.write((pass == null ? "" : pass) + "\n");
			}
		} catch (IOException ignored) {
		}
	}

	private String getPendingPassword() {
		return pendingPassword;
	}

	private void showLeaderboard(String wins, String losses, String draws, String rate) {
		// Keep backwards compatibility if server only sends stats
		String fakeTop = myName + "," + wins + ",0," + draws + "," + rate.replace("%", "");
		showTopLeaderboard(fakeTop);
	}

	private void showHistoryDialog() {
		File f1 = new File("game_history.txt");
		File source = f1.exists() ? f1 : new File("matches.txt");
		if (!source.exists()) {
			JOptionPane.showMessageDialog(this, "Không tìm thấy file lịch sử (game_history.txt hoặc matches.txt).");
			return;
		}

		// Prepare table model (only: Time, Opponent, Result relative to logged-in user)
		String[] cols = { "Thời gian", "Đối thủ", "Kết quả" };
		DefaultTableModel model = new DefaultTableModel(cols, 0) {
			public boolean isCellEditable(int r, int c) {
				return false;
			}
		};

		// Ensure user is logged in
		if (this.myName == null || this.myName.trim().isEmpty()) {
			JOptionPane.showMessageDialog(this, "Vui lòng đăng nhập để xem lịch sử của bạn.");
			return;
		}

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try (BufferedReader br = new BufferedReader(new FileReader(source))) {
			String line;
			while ((line = br.readLine()) != null) {
				if (line.trim().isEmpty())
					continue;
				String time = "", p1 = "", p2 = "", result = "";
				if (source.getName().equalsIgnoreCase("game_history.txt")) {
					// formats: multiple variants
					if (line.startsWith("Game at ")) {
						int idx = line.lastIndexOf(":");
						if (idx > 0) {
							time = line.substring(8, idx).trim();
							result = line.substring(idx + 1).trim();
						} else {
							result = line;
						}
					} else if (line.contains(" - ")) {
						String[] parts = line.split(" - ");
						time = parts.length > 0 ? parts[0].trim() : "";
						if (parts.length > 1)
							result = parts[1].trim();
						for (int i = 2; i < parts.length; i++) {
							String part = parts[i];
							if (part.startsWith("moves:"))
								; // moves info ignored for compact history
							else if (part.contains(" vs ")) {
								String[] pp = part.split(" vs ");
								p1 = pp.length > 0 ? pp[0].trim() : "";
								p2 = pp.length > 1 ? pp[1].trim() : "";
							}
						}
					} else {
						// fallback: try split by ' - '
						result = line;
					}
				} else {
					// parse matches.txt like: p1:p2:result:moves:timestamp
					String[] parts = line.split(":");
					if (parts.length >= 5) {
						p1 = parts[0];
						p2 = parts[1];
						String rc = parts[2];
						// parts[3] moves ignored in compact view
						String ts = parts[4];
						try {
							long t = Long.parseLong(ts);
							time = sdf.format(new Date(t));
						} catch (Exception ex) {
							time = ts;
						}
						if ("1".equals(rc))
							result = "P1 wins";
						else if ("2".equals(rc))
							result = "P2 wins";
						else
							result = rc;
					} else {
						// fallback: no structured players/moves available
					}
				}

				// Filter: only include rows where current logged-in user is involved
				boolean include = false;
				String me = this.myName != null ? this.myName.trim() : "";
				if (!p1.isEmpty() || !p2.isEmpty()) {
					if (me.equalsIgnoreCase(p1) || me.equalsIgnoreCase(p2))
						include = true;
				} else {
					if (!me.isEmpty() && line.toLowerCase().contains(me.toLowerCase()))
						include = true;
				}

				if (include) {
					// determine opponent and relative result
					String opponent = "";
					String relResult = ""; // Thắng / Thua / Hòa / fallback raw
					if (!p1.isEmpty() || !p2.isEmpty()) {
						if (me.equalsIgnoreCase(p1))
							opponent = p2;
						else if (me.equalsIgnoreCase(p2))
							opponent = p1;
					}

					// Normalize result to determine relative outcome
					String low = result == null ? "" : result.toLowerCase();
					if (low.contains("draw") || low.contains("hòa") || low.contains("hoa") || low.contains("tie")) {
						relResult = "Hòa";
					} else if (low.contains("p1 wins") || low.contains("p2 wins") || low.contains("wins")
							|| low.contains("thắng") || low.contains("win")) {
						// If we know which player won via p1/p2 or rc mapping, map to Thắng/Thua
						boolean determined = false;
						// For matches.txt we used result like "P1 wins" or set via rc; if result
						// contains p1/p2 winner
						if (!p1.isEmpty() && !p2.isEmpty()) {
							if (low.contains("p1") && me.equalsIgnoreCase(p1)) {
								relResult = "Thắng";
								determined = true;
							} else if (low.contains("p1") && me.equalsIgnoreCase(p2)) {
								relResult = "Thua";
								determined = true;
							} else if (low.contains("p2") && me.equalsIgnoreCase(p2)) {
								relResult = "Thắng";
								determined = true;
							} else if (low.contains("p2") && me.equalsIgnoreCase(p1)) {
								relResult = "Thua";
								determined = true;
							}
						}
						if (!determined) {
							// Heuristic: if result contains 'win' and moves or result mentions my name ->
							// Thắng
							if (!me.isEmpty() && low.contains(me.toLowerCase()))
								relResult = "Thắng";
							else
								relResult = low.contains("win") || low.contains("thắng") ? "Thắng" : "Thua";
						}
					} else {
						// fallback: use raw result
						relResult = result != null && !result.isEmpty() ? result : "";
					}

					if (opponent == null)
						opponent = "";
					model.addRow(new Object[] { time, opponent, relResult });
				}
			}
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, "Lỗi đọc file lịch sử: " + e.getMessage());
			return;
		}

		if (model.getRowCount() == 0) {
			JOptionPane.showMessageDialog(this, "Không tìm thấy lịch sử trận đấu cho người chơi: " + this.myName);
			return;
		}

		JTable table = new JTable(model);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		JScrollPane sp = new JScrollPane(table);
		sp.setPreferredSize(new Dimension(760, 420));

		JDialog dialog = new JDialog(this, "Lịch sử đấu của " + this.myName, true);
		dialog.setLayout(new BorderLayout(8, 8));
		dialog.add(sp, BorderLayout.CENTER);
		JButton close = new JButton("Đóng");
		close.addActionListener(e -> dialog.dispose());
		dialog.add(close, BorderLayout.SOUTH);
		dialog.pack();
		dialog.setLocationRelativeTo(this);
		dialog.setVisible(true);
	}

	private void ensureLeaderboardDialog() {
		if (leaderboardDialog != null)
			return;
		leaderboardDialog = new JDialog(this, "Bảng xếp hạng top cao thủ", true);
		leaderboardDialog.setLayout(new BorderLayout(0, 10));
		leaderboardDialog.setSize(760, 460);
		leaderboardDialog.setLocationRelativeTo(this);

		// Header panel với gradient
		JPanel headerPanel = UIEffects.createGradientPanel(
				new Color(52, 152, 219),
				new Color(41, 128, 185),
				true);
		headerPanel.setLayout(new BorderLayout());
		headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

		JLabel header = new JLabel("🏆 TOP CAO THỦ 🏆", SwingConstants.CENTER);
		header.setFont(new Font("Segoe UI", Font.BOLD, 24));
		header.setForeground(Color.WHITE);
		headerPanel.add(header, BorderLayout.CENTER);
		leaderboardDialog.add(headerPanel, BorderLayout.NORTH);

		// Table với giao diện mới
		String[] cols = { "Hạng", "Tên người chơi", "Trận thắng", "Trận thua", "Tỷ lệ thắng" };
		leaderboardModel = new DefaultTableModel(cols, 0) {
			public boolean isCellEditable(int r, int c) {
				return false;
			}
		};
		leaderboardTable = new JTable(leaderboardModel) {
			@Override
			public Component prepareRenderer(TableCellRenderer renderer, int row, int col) {
				Component c = super.prepareRenderer(renderer, row, col);
				if (row < 3) { // Top 3 rows
					c.setFont(new Font("Segoe UI", Font.BOLD, 14));
					switch (row) {
						case 0:
							c.setForeground(new Color(255, 215, 0));
							break; // Gold
						case 1:
							c.setForeground(new Color(192, 192, 192));
							break; // Silver
						case 2:
							c.setForeground(new Color(205, 127, 50));
							break; // Bronze
					}
				} else {
					c.setFont(new Font("Segoe UI", Font.PLAIN, 13));
					c.setForeground(Color.BLACK);
				}
				if (row % 2 == 0) {
					c.setBackground(new Color(240, 248, 255));
				} else {
					c.setBackground(Color.WHITE);
				}
				return c;
			}
		};

		// Cấu hình table
		leaderboardTable.setRowHeight(45);
		leaderboardTable.setShowGrid(true); // Bảng kẻ phân chia rõ ràng
		leaderboardTable.setGridColor(new Color(210, 220, 230));
		leaderboardTable.setIntercellSpacing(new Dimension(1, 1));
		leaderboardTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
		leaderboardTable.getTableHeader().setBackground(new Color(41, 128, 185));
		leaderboardTable.getTableHeader().setForeground(Color.WHITE);
		leaderboardTable.setSelectionBackground(new Color(230, 240, 250));
		leaderboardTable.setSelectionForeground(Color.BLACK);

		// Renderer cho cột "Hạng" với huy chương
		leaderboardTable.getColumnModel().getColumn(0)
				.setCellRenderer((table, value, isSelected, hasFocus, row, column) -> {
					JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
					panel.setOpaque(true);
					JLabel iconLbl = new JLabel();
					int r = row;
					// Ưu tiên ảnh cung cấp cho Top1/Top2/Top3, fallback về huy chương vẽ
					ImageIcon badge = loadTopBadgeIcon(r);
					iconLbl.setIcon(badge != null ? badge : (r < 3 ? createMedalIcon(r) : null));
					panel.add(iconLbl);
					// Top 1-3 chỉ ảnh, từ top 4 trở đi chỉ hiển thị số
					if (r >= 3) {
						JLabel text = new JLabel(String.valueOf(value));
						text.setFont(new Font("Segoe UI", Font.PLAIN, 13));
						panel.add(text);
					}
					if (isSelected) {
						panel.setBackground(new Color(230, 240, 250));
					} else {
						panel.setBackground(row % 2 == 0 ? new Color(245, 250, 255) : Color.WHITE);
					}
					return panel;
				});

		// Không dùng avatar cạnh người chơi -> trả renderer mặc định cho cột tên
		leaderboardTable.getColumnModel().getColumn(1)
				.setCellRenderer(new javax.swing.table.DefaultTableCellRenderer());

		JScrollPane scroll = new JScrollPane(leaderboardTable);
		scroll.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
		scroll.getViewport().setBackground(Color.WHITE);
		// Content panel để chèn subheader phía trên bảng
		JPanel contentPanel = new JPanel(new BorderLayout());
		JLabel subHeader = new JLabel("BẢNG XẾP HẠNG TỔNG", SwingConstants.CENTER);
		subHeader.setFont(new Font("Segoe UI", Font.BOLD, 16));
		subHeader.setForeground(new Color(52, 73, 94));
		subHeader.setBorder(BorderFactory.createEmptyBorder(8, 0, 4, 0));
		contentPanel.add(subHeader, BorderLayout.NORTH);
		contentPanel.add(scroll, BorderLayout.CENTER);
		leaderboardDialog.add(contentPanel, BorderLayout.CENTER);

		// Panel chứa các nút với gradient
		JPanel actions = UIEffects.createGradientPanel(
				new Color(240, 248, 255),
				new Color(230, 240, 250),
				true);
		actions.setLayout(new FlowLayout(FlowLayout.RIGHT, 20, 15));
		actions.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 20));

		JButton refreshBtn = UIEffects.createGradientButton(
				"🔄 Làm mới",
				new Color(52, 152, 219),
				new Color(41, 128, 185));
		refreshBtn.setPreferredSize(new Dimension(130, 35));

		JButton closeBtn = UIEffects.createGradientButton(
				"❌ Đóng",
				new Color(231, 76, 60),
				new Color(192, 57, 43));
		closeBtn.setPreferredSize(new Dimension(130, 35));

		refreshBtn.addActionListener(e -> {
			if (out != null)
				out.println("get_top");
		});
		closeBtn.addActionListener(e -> leaderboardDialog.setVisible(false));

		// Thêm hiệu ứng hover cho các nút
		UIEffects.addHoverEffect(refreshBtn);
		UIEffects.addHoverEffect(closeBtn);

		actions.add(refreshBtn);
		actions.add(closeBtn);
		leaderboardDialog.add(actions, BorderLayout.SOUTH);
	}

	private void showTopLeaderboard(String payload) {
		ensureLeaderboardDialog();
		leaderboardModel.setRowCount(0);
		if (payload != null && !payload.isEmpty()) {
			String[] rows = payload.split("\\|");
			int rank = 1;
			for (String r : rows) {
				String[] cols = r.split(",");
				if (cols.length >= 5) {
					String user = cols[0];
					String wins = cols[1];
					String losses = cols[2];
					// String draws = cols[3]; // available if needed later
					String rate = cols[4] + "%";
					leaderboardModel.addRow(new Object[] { rank++, user, wins, losses, rate });
				}
			}
		}
		leaderboardDialog.setVisible(true);
	}

	// === Icon Helpers for Leaderboard ===
	private ImageIcon createMedalIcon(int rankIndex) {
		Color color;
		if (rankIndex == 0)
			color = new Color(255, 215, 0); // Gold
		else if (rankIndex == 1)
			color = new Color(192, 192, 192); // Silver
		else if (rankIndex == 2)
			color = new Color(205, 127, 50); // Bronze
		else
			color = new Color(120, 144, 156); // Gray

		int w = 28, h = 28;
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = img.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setColor(color);
		g2.fillOval(1, 1, w - 2, h - 2);
		g2.setColor(new Color(255, 255, 255, 180));
		g2.setStroke(new BasicStroke(2f));
		g2.drawOval(1, 1, w - 2, h - 2);
		// Số thứ hạng
		g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
		g2.setColor(new Color(60, 50, 30));
		String num = rankIndex == 0 ? "1" : rankIndex == 1 ? "2" : rankIndex == 2 ? "3" : "";
		FontMetrics fm = g2.getFontMetrics();
		int tx = (w - fm.stringWidth(num)) / 2;
		int ty = (h + fm.getAscent() - fm.getDescent()) / 2 - 1;
		g2.drawString(num, tx, ty);
		g2.dispose();
		return new ImageIcon(img);
	}

	// Cố gắng load ảnh top1/top2/top3 từ thư mục 'docs' nếu có
	private ImageIcon loadTopBadgeIcon(int rankIndex) {
		String name = (rankIndex == 0) ? "top1" : (rankIndex == 1) ? "top2" : (rankIndex == 2) ? "top3" : null;
		if (name == null)
			return null;
		String[] candidates = new String[] {
				"docs/" + name + ".png",
				"docs/" + name + ".jpg",
				"docs/" + name + ".jpeg"
		};
		for (String path : candidates) {
			try {
				File f = new File(path);
				if (f.exists()) {
					Image img = javax.imageio.ImageIO.read(f);
					if (img != null) {
						Image scaled = img.getScaledInstance(28, 28, Image.SCALE_SMOOTH);
						return new ImageIcon(scaled);
					}
				}
			} catch (Exception ignore) {
			}
		}
		return null;
	}

	// === Matchmaking dialog ===
	private void showMatchmakingDialog() {
		if (isMatchmaking || isInGame)
			return; // Không hiển thị dialog nếu đang ghép trận hoặc đang trong game
		isMatchmaking = true; // Đánh dấu đang trong trạng thái ghép trận
		hideMatchmakingDialog(); // Đảm bảo dialog cũ được đóng hoàn toàn
		if (matchmakingDialog == null) {
			matchmakingDialog = new JDialog(this, "Đang ghép trận", false); // Đặt là non-modal để timer hoạt động
			matchmakingDialog.setAlwaysOnTop(true); // Luôn hiển thị trên cùng
			matchmakingDialog.setSize(360, 160);
			matchmakingDialog.setLocationRelativeTo(this);
			JPanel panel = new JPanel(new BorderLayout(10, 10));
			panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

			// Label thông báo với font lớn hơn
			matchmakingLabel = new JLabel("Đang tìm đối thủ...", SwingConstants.CENTER);
			matchmakingLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
			matchmakingLabel.setForeground(new Color(41, 128, 185));

			// Nút hủy với thiết kế nổi bật
			JButton cancel = UIEffects.createGradientButton("Hủy ghép trận", new Color(231, 76, 60),
					new Color(192, 57, 43));
			cancel.setFont(new Font("Segoe UI", Font.BOLD, 14));
			cancel.setPreferredSize(new Dimension(200, 40));
			UIEffects.addHoverEffect(cancel);
			cancel.addActionListener(e -> {
				if (out != null) {
					out.println("cancel_quick_play");
					isMatchmaking = false; // Reset trạng thái ghép trận
					hideMatchmakingDialog();
					rootLayout.show(rootPanel, "LOBBY"); // Quay về màn hình chính khi hủy ghép trận
				}
			});
			panel.add(matchmakingLabel, BorderLayout.CENTER);
			panel.add(cancel, BorderLayout.SOUTH);
			matchmakingDialog.setContentPane(panel);
		}
		// Đã bỏ phần đếm giây
		matchmakingDialog.setVisible(true);
		matchmakingDialog.requestFocus(); // Đảm bảo dialog được focus
	}

	private void hideMatchmakingDialog() {
		isMatchmaking = false; // Reset trạng thái ghép trận
		// Ẩn/đóng dialog nếu có
		if (matchmakingDialog != null) {
			matchmakingDialog.dispose(); // Đóng hoàn toàn dialog thay vì chỉ ẩn
			matchmakingDialog = null; // Hủy reference để tạo mới lần sau
		}
		// Ẩn nút hủy ghép trận trên giao diện chính nếu tồn tại
		if (cancelMatchButton != null) {
			cancelMatchButton.setVisible(false);
		}
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			try {
				new CaroClient();
			} catch (Exception e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(null, "Lỗi khởi tạo giao diện: " + e.getMessage());
			}
		});
	}

	// Fallback UIEffects if external class is not compiled in this project
	static class UIEffects {
		static JPanel createGradientPanel(Color startColor, Color endColor, boolean withShadow) {
			return new JPanel();
		}

		static JButton createGradientButton(String text, Color startColor, Color endColor) {
			JButton b = new JButton(text);
			b.setBackground(startColor);
			b.setForeground(Color.WHITE);
			return b;
		}

		static void addHoverEffect(JButton button) {
			// no-op fallback
		}

		static Border createAnimatedBorder() {
			return BorderFactory.createLineBorder(new Color(200, 200, 200));
		}
	}
}