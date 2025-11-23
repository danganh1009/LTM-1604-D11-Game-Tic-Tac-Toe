package Server;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class CaroServer {
    private static final int PORT = 8001;
    private static final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private static volatile int nextGuestId = 1;
    private static final ConcurrentLinkedQueue<ClientHandler> matchmakingQueue = new ConcurrentLinkedQueue<>();

    public static void main(String[] args) {
        System.out.println("CaroServer starting on port " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket s = serverSocket.accept();
                ClientHandler h = new ClientHandler(s);
                new Thread(h, "client-" + h.getId()).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Broadcast current online usernames with status
    private static void broadcastOnline() {
        StringBuilder sb = new StringBuilder();
        sb.append("online:");
        boolean first = true;
        for (Map.Entry<String, ClientHandler> e : clients.entrySet()) {
            if (!first)
                sb.append(",");
            ClientHandler h = e.getValue();
            sb.append(e.getKey()).append("|").append(h.isBusy() ? "busy" : "free");
            first = false;
        }
        String msg = sb.toString();
        for (ClientHandler h : clients.values()) {
            h.send(msg);
        }
    }

    private static synchronized void appendHistory(String entry) {
        try (FileWriter fw = new FileWriter("game_history.txt", true)) {
            fw.write(entry + System.lineSeparator());
        } catch (IOException e) {
            System.out.println("Failed to append history: " + e.getMessage());
        }
    }

    private static String computeStatsForPlayer(String name) {
        int wins = 0, losses = 0, draws = 0;
        File f = new File("game_history.txt");
        if (!f.exists())
            return "wins=0;losses=0;draws=0;rate=0%";
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(" - ");
                if (parts.length < 3)
                    continue;
                String result = parts[1].trim();
                String names = parts[2].trim();
                String[] two = names.split(" vs ", 2);
                String g1 = two.length > 0 ? two[0].trim() : "";
                String g2 = two.length > 1 ? two[1].trim() : "";
                if (result.equalsIgnoreCase("Draw")) {
                    if (name.equals(g1) || name.equals(g2))
                        draws++;
                } else if (result.equals("P1 wins")) {
                    if (name.equals(g1))
                        wins++;
                    else if (name.equals(g2))
                        losses++;
                } else if (result.equals("P2 wins")) {
                    if (name.equals(g2))
                        wins++;
                    else if (name.equals(g1))
                        losses++;
                } else {
                    if (result.contains(name) && result.toLowerCase().contains("wins"))
                        wins++;
                }
            }
        } catch (IOException e) {
            return "wins=0;losses=0;draws=0;rate=0%";
        }
        int played = wins + losses;
        int rate = played == 0 ? 0 : (int) Math.round((wins * 100.0) / played);
        return "wins=" + wins + ";losses=" + losses + ";draws=" + draws + ";rate=" + rate + "%";
    }

    // === ClientHandler ===
    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String name;
        private final int id;
        private ClientHandler opponent;
        private GameSession session;
        private BotSession botSession;
        private volatile boolean busy = false;

        ClientHandler(Socket s) throws IOException {
            this.socket = s;
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.id = nextGuestId++;
            this.name = "Guest" + id;
        }

        int getId() {
            return id;
        }

        void send(String msg) {
            out.println(msg);
        }

        boolean isBusy() {
            return busy;
        }

        @Override
        public void run() {
            try {
                send("welcome:Please send login:<name>");
                String line;
                clients.put(name, this);
                broadcastOnline();
                while ((line = in.readLine()) != null) {
                    if (line.startsWith("login:")) {
                        String newName = line.substring(6).trim();
                        if (newName.isEmpty())
                            newName = "Guest" + id;
                        clients.remove(this.name);
                        this.name = newName;
                        clients.put(this.name, this);
                        broadcastOnline();
                    } else if (line.startsWith("register:")) {
                        String[] parts = line.split(":", 3);
                        if (parts.length < 3) {
                            send("error:Invalid register");
                            continue;
                        }
                        String u = parts[1].trim();
                        String p = parts[2];
                        String res = registerUserFile(u, p);
                        if ("ok".equals(res))
                            send("register_ok");
                        else if ("dup".equals(res))
                            send("register_dup");
                        else
                            send("error:Register failed");
                    } else if (line.startsWith("auth:")) {
                        String[] parts = line.split(":", 3);
                        if (parts.length < 3) {
                            send("error:Invalid auth");
                            continue;
                        }
                        String u = parts[1].trim();
                        String p = parts[2];
                        if (authenticateFile(u, p)) {
                            clients.remove(this.name);
                            this.name = u;
                            clients.put(this.name, this);
                            send("auth_ok:" + u);
                            broadcastOnline();
                        } else {
                            send("auth_fail");
                        }
                    } else if (line.startsWith("get_stats")) {
                        send("stats:" + computeStatsForPlayer(name));
                    } else if (line.startsWith("get_top")) {
                        send("top:" + getTopPlayersFile(50));
                    } else if (line.equals("quick_play")) {
                        if (!busy) {
                            matchmakingQueue.add(this);
                            busy = true;
                            send("wait");
                            checkMatchmaking();
                        }
                    } else if (line.equals("cancel_quick_play")) {
                        // Cho phép hủy ghép trận: loại khỏi hàng đợi và mở khóa
                        if (busy && session == null && botSession == null) {
                            matchmakingQueue.remove(this);
                            busy = false;
                            send("cancelled");
                            broadcastOnline();
                        }
                    } else if (line.equals("play_with_bot")) {
                        if (!busy) {
                            busy = true;
                            this.botSession = new BotSession(this);
                            botSession.start();
                        }
                    } else if (line.startsWith("challenge:")) {
                        if (busy)
                            continue;
                        String target = line.substring(10).trim();
                        ClientHandler targetHandler = clients.get(target);
                        if (targetHandler != null && !targetHandler.busy && !target.equals(name)) {
                            targetHandler.send("invite:" + name);
                        } else {
                            send("error:Target not available");
                        }
                    } else if (line.startsWith("accept:")) {
                        if (busy)
                            continue;
                        String challenger = line.substring(7).trim();
                        ClientHandler challengerHandler = clients.get(challenger);
                        if (challengerHandler != null && !challengerHandler.busy) {
                            busy = true;
                            challengerHandler.busy = true;
                            opponent = challengerHandler;
                            challengerHandler.opponent = this;
                            GameSession newSession = new GameSession(challengerHandler, this);
                            this.session = newSession;
                            challengerHandler.session = newSession;
                            session.start();
                        }
                    } else if (line.startsWith("reject:")) {
                        String challenger = line.substring(7).trim();
                        ClientHandler challengerHandler = clients.get(challenger);
                        if (challengerHandler != null) {
                            challengerHandler.send("rejected:" + name);
                        }
                    } else if (line.startsWith("chat:")) {
                        if (session != null) {
                            session.handleChat(this, line.substring(5));
                        }
                    } else if (line.startsWith("save_game")) {
                        if (session != null) {
                            String saveData = session.saveGame();
                            String filename = "game_" + System.currentTimeMillis() + ".sav";
                            try (FileWriter fw = new FileWriter(filename)) {
                                fw.write(saveData);
                                send("game_saved:" + filename);
                            } catch (IOException e) {
                                send("error:Không thể lưu game");
                            }
                        }
                    } else if (line.startsWith("load_game:")) {
                        String filename = line.substring(10);
                        try {
                            StringBuilder content = new StringBuilder();
                            try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
                                String l;
                                while ((l = br.readLine()) != null) {
                                    content.append(l).append("\n");
                                }
                            }
                            if (opponent != null) {
                                GameSession loadedSession = GameSession.loadGame(content.toString(), this, opponent);
                                if (loadedSession != null) {
                                    this.session = loadedSession; // Set loaded session for this player
                                    opponent.session = loadedSession; // Set loaded session for opponent
                                    loadedSession.start();
                                } else {
                                    send("error:Không thể tải game");
                                }
                            }
                        } catch (IOException e) {
                            send("error:Không thể đọc file game");
                        }
                    } else if (line.startsWith("move:")) {
                        if (session != null) {
                            session.handleMove(this, line.substring(5));
                        } else if (botSession != null) {
                            botSession.handleMove(line.substring(5));
                        }
                    } else if (line.equals("play_again")) {
                        if (session != null)
                            session.requestPlayAgain(this);
                        else if (botSession != null)
                            botSession.requestPlayAgain();
                    } else if (line.equals("leave")) {
                        if (session != null)
                            session.handleLeave(this);
                        if (botSession != null)
                            botSession.handleLeave();
                    }
                }
            } catch (IOException e) {
                System.out.println("Client " + name + " disconnected: " + e.getMessage());
            } finally {
                clients.remove(name);
                if (session != null)
                    session.handleLeave(this);
                if (botSession != null)
                    botSession.handleLeave();
                matchmakingQueue.remove(this);
                broadcastOnline();
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }
    }

    // === Matchmaking ===
    private static void checkMatchmaking() {
        while (matchmakingQueue.size() >= 2) {
            ClientHandler p1 = matchmakingQueue.poll();
            ClientHandler p2 = matchmakingQueue.poll();
            if (p1 != null && p2 != null) {
                GameSession session = new GameSession(p1, p2);
                p1.session = session;
                p2.session = session;
                p1.opponent = p2;
                p2.opponent = p1;
                p1.busy = true;
                p2.busy = true;
                session.start();
                broadcastOnline();
            }
        }
    }

    // === GameSession Người vs Người ===
    private static class GameSession {
        private ClientHandler p1;
        private ClientHandler p2;
        private byte[] board = new byte[9];
        private int currentPlayer = 1;
        private StringBuilder moves = new StringBuilder();
        private boolean ended = false;
        private boolean p1Play = false, p2Play = false;
        private List<String> chatHistory = new ArrayList<>();
        private LocalDateTime startTime;

        GameSession(ClientHandler player1, ClientHandler player2) {
            p1 = player1;
            p2 = player2;
        }

        void start() {
            startTime = LocalDateTime.now();
            p1.send("start:X");
            p2.send("start:O");
            p1.send("your_turn");
            p2.send("wait");
            // Gửi tin nhắn chào mừng
            String welcomeMsg = "Game bắt đầu! " + p1.name + " (X) vs " + p2.name + " (O)";
            sendChatMessage("System", welcomeMsg);
        }

        void sendChatMessage(String sender, String message) {
            String chatMsg = "chat:" + sender + ":" + message;
            chatHistory.add(chatMsg);
            p1.send(chatMsg);
            p2.send(chatMsg);
        }

        void handleChat(ClientHandler sender, String message) {
            sendChatMessage(sender.name, message);
        }

        String saveGame() {
            StringBuilder sb = new StringBuilder();
            sb.append("version:1\n");
            sb.append("start_time:").append(startTime).append("\n");
            sb.append("p1:").append(p1.name).append("\n");
            sb.append("p2:").append(p2.name).append("\n");
            sb.append("current:").append(currentPlayer).append("\n");
            sb.append("board:").append(Arrays.toString(board)).append("\n");
            sb.append("moves:").append(moves.toString()).append("\n");
            return sb.toString();
        }

        static GameSession loadGame(String data, ClientHandler p1, ClientHandler p2) {
            try {
                GameSession session = new GameSession(p1, p2);
                String[] lines = data.split("\n");
                for (String line : lines) {
                    String[] parts = line.split(":", 2);
                    if (parts.length != 2)
                        continue;

                    switch (parts[0]) {
                        case "current":
                            session.currentPlayer = Integer.parseInt(parts[1]);
                            break;
                        case "board":
                            String boardStr = parts[1].substring(1, parts[1].length() - 1);
                            String[] boardParts = boardStr.split(", ");
                            for (int i = 0; i < 9 && i < boardParts.length; i++) {
                                session.board[i] = Byte.parseByte(boardParts[i]);
                            }
                            break;
                        case "moves":
                            session.moves = new StringBuilder(parts[1]);
                            break;
                    }
                }
                return session;
            } catch (Exception e) {
                System.out.println("Lỗi tải game: " + e.getMessage());
                return null;
            }
        }

        synchronized void handleMove(ClientHandler who, String move) {
            String[] parts = move.split(",");
            int row = Integer.parseInt(parts[0]);
            int col = Integer.parseInt(parts[1]);
            int index = row * 3 + col;
            if (board[index] != 0 || ended)
                return;
            if (who == p1 && currentPlayer == 1 || who == p2 && currentPlayer == 2) {
                board[index] = (byte) currentPlayer;
                moves.append(row + "," + col + ";");
                ClientHandler other = (who == p1) ? p2 : p1;
                other.send("move:" + move);
                if (checkWin(currentPlayer)) {
                    who.send("win");
                    other.send("lose");
                    ended = true;
                    int result = (currentPlayer == 1) ? 1 : 2;
                    storeMatchResultFile(p1.name, p2.name, result, moves.toString());
                    appendHistory(
                            LocalDateTime.now() + " - P" + currentPlayer + " wins - " + p1.name + " vs " + p2.name);
                } else if (isDraw()) {
                    p1.send("draw");
                    p2.send("draw");
                    ended = true;
                    storeMatchResultFile(p1.name, p2.name, 0, moves.toString());
                    appendHistory(LocalDateTime.now() + " - Draw - " + p1.name + " vs " + p2.name);
                } else {
                    currentPlayer = 3 - currentPlayer;
                    ClientHandler next = (currentPlayer == 1) ? p1 : p2;
                    ClientHandler wait = (currentPlayer == 1) ? p2 : p1;
                    next.send("your_turn");
                    wait.send("wait");
                }
            }
        }

        private boolean checkWin(int player) {
            byte m = (byte) player;
            for (int i = 0; i < 3; i++) {
                if (board[i * 3] == m && board[i * 3 + 1] == m && board[i * 3 + 2] == m)
                    return true;
                if (board[i] == m && board[i + 3] == m && board[i + 6] == m)
                    return true;
            }
            if (board[0] == m && board[4] == m && board[8] == m)
                return true;
            if (board[2] == m && board[4] == m && board[6] == m)
                return true;
            return false;
        }

        private boolean isDraw() {
            for (byte b : board)
                if (b == 0)
                    return false;
            return true;
        }

        private void resetBoard() {
            board = new byte[9];
            moves.setLength(0);
            ended = false;
        }

        synchronized void requestPlayAgain(ClientHandler who) {
            if (who == p1)
                p1Play = true;
            else
                p2Play = true;

            ClientHandler other = (who == p1) ? p2 : p1;

            if (p1Play && p2Play) {
                resetBoard();
                currentPlayer = 1;
                p1Play = false;
                p2Play = false;
                p1.send("restart");
                p2.send("restart");
                p1.send("your_turn");
                p2.send("wait");
            } else {
                other.send("opponent_wants_rematch");
            }
        }

        synchronized void handleLeave(ClientHandler who) {
            ClientHandler other = (who == p1) ? p2 : p1;
            if (other != null) {
                other.send("opponent_left:" + who.name);
                other.session = null;
                other.opponent = null;
                other.busy = false;
            }
            who.session = null;
            who.opponent = null;
            who.busy = false;
            broadcastOnline();
        }
    }

    // === BotSession Người vs Máy ===
    private static class BotSession {
        private ClientHandler player;
        private byte[] board = new byte[9];
        private boolean ended = false;

        BotSession(ClientHandler p) {
            this.player = p;
        }

        void start() {
            player.send("start:X");
            player.send("your_turn");
        }

        synchronized void handleMove(String move) {
            if (ended)
                return;
            String[] parts = move.split(",");
            int row = Integer.parseInt(parts[0]);
            int col = Integer.parseInt(parts[1]);
            int index = row * 3 + col;
            if (board[index] != 0)
                return;

            // Người đánh
            board[index] = 1;
            if (checkWin(1)) {
                player.send("win");
                ended = true;
                return;
            }
            if (isDraw()) {
                player.send("draw");
                ended = true;
                return;
            }

            // Bot đánh
            int botIndex = getRandomEmpty();
            board[botIndex] = 2;
            int r = botIndex / 3, c = botIndex % 3;
            player.send("move:" + r + "," + c);
            if (checkWin(2)) {
                player.send("lose");
                ended = true;
            } else if (isDraw()) {
                player.send("draw");
                ended = true;
            } else {
                player.send("your_turn");
            }
        }

        private boolean checkWin(int mark) {
            byte m = (byte) mark;
            for (int i = 0; i < 3; i++) {
                if (board[i * 3] == m && board[i * 3 + 1] == m && board[i * 3 + 2] == m)
                    return true;
                if (board[i] == m && board[i + 3] == m && board[i + 6] == m)
                    return true;
            }
            if (board[0] == m && board[4] == m && board[8] == m)
                return true;
            if (board[2] == m && board[4] == m && board[6] == m)
                return true;
            return false;
        }

        private boolean isDraw() {
            for (byte b : board)
                if (b == 0)
                    return false;
            return true;
        }

        private int getRandomEmpty() {
            List<Integer> empty = new ArrayList<>();
            for (int i = 0; i < 9; i++)
                if (board[i] == 0)
                    empty.add(i);
            return empty.get(new Random().nextInt(empty.size()));
        }

        private void reset() {
            board = new byte[9];
            ended = false;
        }

        synchronized void requestPlayAgain() {
            reset();
            player.send("restart");
            player.send("your_turn");
        }

        synchronized void handleLeave() {
            player.session = null;
            player.botSession = null;
            player.busy = false;
        }
    }

    // === File-based storage ===
    static String registerUserFile(String username, String password) {
        if (username == null || username.isEmpty() || password == null || password.isEmpty())
            return "fail";
        try {
            String salt = generateSalt();
            String hash = hash(password, salt);
            String line = username + ":" + hash + ":" + salt;
            File f = new File("users.txt");
            if (f.exists()) {
                try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                    String l;
                    while ((l = br.readLine()) != null) {
                        if (l.startsWith(username + ":"))
                            return "dup";
                    }
                }
            }
            try (FileWriter fw = new FileWriter(f, true)) {
                fw.write(line + "\n");
            }
            return "ok";
        } catch (Exception e) {
            return "fail";
        }
    }

    static boolean authenticateFile(String username, String password) {
        if (username == null || password == null)
            return false;
        try {
            File f = new File("users.txt");
            if (!f.exists())
                return false;
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String l;
                while ((l = br.readLine()) != null) {
                    String[] p = l.split(":", 3);
                    if (p.length == 3 && p[0].equals(username)) {
                        String stored = p[1], salt = p[2];
                        String computed = hash(password, salt);
                        return stored.equals(computed);
                    }
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    static void storeMatchResultFile(String p1, String p2, int result, String moves) {
        try (FileWriter fw = new FileWriter("game_history.txt", true)) {
            String res = (result == 1) ? "P1 wins" : (result == 2) ? "P2 wins" : "Draw";
            fw.write(LocalDateTime.now() + " - " + res + " - " + p1 + " vs " + p2 + " - " + moves + "\n");
        } catch (IOException e) {
            System.out.println("Failed to store match result: " + e.getMessage());
        }
    }

    // === Utils for hashing passwords ===
    private static String generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    private static String hash(String password, String salt) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(Base64.getDecoder().decode(salt));
        byte[] hashed = md.digest(password.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(hashed);
    }

    private static String getTopPlayersFile(int n) {
        // Build full stats (wins, losses, draws, rate) per player from history file
        File f = new File("game_history.txt");
        if (!f.exists())
            return "";

        // Load registered usernames (only rank logged-in/registered accounts)
        Set<String> registeredUsers = new HashSet<>();
        File usersFile = new File("users.txt");
        if (usersFile.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(usersFile))) {
                String l;
                while ((l = br.readLine()) != null) {
                    String[] p = l.split(":", 2);
                    if (p.length >= 1 && !p[0].trim().isEmpty()) {
                        registeredUsers.add(p[0].trim());
                    }
                }
            } catch (IOException ignore) {
            }
        }

        class Stats {
            int wins = 0;
            int losses = 0;
            int draws = 0;
            int winRate() {
                int played = wins + losses;
                return played == 0 ? 0 : (int) Math.round((wins * 100.0) / played);
            }
        }

        Map<String, Stats> statsMap = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(" - ");
                if (parts.length < 3)
                    continue;
                String result = parts[1].trim();
                String[] two = parts[2].split(" vs ");
                String p1 = two.length > 0 ? two[0].trim() : "";
                String p2 = two.length > 1 ? two[1].trim() : "";
                if (p1.isEmpty() || p2.isEmpty())
                    continue;

                // Only count matches for registered accounts when building stats
                boolean p1Registered = registeredUsers.isEmpty() ? true : registeredUsers.contains(p1);
                boolean p2Registered = registeredUsers.isEmpty() ? true : registeredUsers.contains(p2);
                if (!p1Registered && !p2Registered) {
                    continue;
                }

                if (p1Registered) statsMap.putIfAbsent(p1, new Stats());
                if (p2Registered) statsMap.putIfAbsent(p2, new Stats());

                if (result.equalsIgnoreCase("Draw")) {
                    if (p1Registered) statsMap.get(p1).draws++;
                    if (p2Registered) statsMap.get(p2).draws++;
                } else if (result.equals("P1 wins")) {
                    if (p1Registered) statsMap.get(p1).wins++;
                    if (p2Registered) statsMap.get(p2).losses++;
                } else if (result.equals("P2 wins")) {
                    if (p2Registered) statsMap.get(p2).wins++;
                    if (p1Registered) statsMap.get(p1).losses++;
                }
            }
        } catch (IOException e) {
            // ignore and fallback to empty
        }

        // Sort by win rate desc, then wins desc, then losses asc, then name asc
        List<Map.Entry<String, Stats>> sorted = new ArrayList<>(statsMap.entrySet());
        sorted.sort((a, b) -> {
            int r = Integer.compare(b.getValue().winRate(), a.getValue().winRate());
            if (r != 0) return r;
            r = Integer.compare(b.getValue().wins, a.getValue().wins);
            if (r != 0) return r;
            r = Integer.compare(a.getValue().losses, b.getValue().losses);
            if (r != 0) return r;
            return a.getKey().compareToIgnoreCase(b.getKey());
        });

        StringBuilder sb = new StringBuilder();
        int limit = Math.min(n, sorted.size());
        for (int i = 0; i < limit; i++) {
            Map.Entry<String, Stats> e = sorted.get(i);
            Stats s = e.getValue();
            if (i > 0) sb.append("|");
            sb.append(e.getKey()).append(",")
              .append(s.wins).append(",")
              .append(s.losses).append(",")
              .append(s.draws).append(",")
              .append(s.winRate());
        }
        return sb.toString();
    }
}
