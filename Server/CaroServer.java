package Server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.sql.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CaroServer {
    private static final int PORT = 8000;
    private static final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private static volatile int nextGuestId = 1;

    public static void main(String[] args) {
        System.out.println("CaroServer starting on port " + PORT);
        Db.init();
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

    // Broadcast current online usernames with status (name|busy or name|free) to all clients
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
    // Append game history line
    private static synchronized void appendHistory(String entry) {
        try (FileWriter fw = new FileWriter("game_history.txt", true)) {
            fw.write(entry + System.lineSeparator());
        } catch (IOException e) {
            System.out.println("Failed to append history: " + e.getMessage());
        }
    }

    // Compute stats (file fallback)
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

    // === ClientHandler inner class ===
    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String name;
        private final int id;
        private ClientHandler opponent;
        private GameSession session;
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
                        // rename in clients map
                        clients.remove(this.name);
                        this.name = newName;
                        clients.put(this.name, this);
                        Db.ensureUser(this.name);
                        broadcastOnline();
                    } else if (line.startsWith("get_stats")) {
                        String stats = Db.computeStatsForPlayerOrFallback(name);
                        send("stats:" + stats);
                    } else if (line.startsWith("challenge:")) {
                        String target = line.substring(10).trim();
                        ClientHandler t = clients.get(target);
                        if (t != null && t != this) {
                            if (this.busy) {
                                send("error:you_are_busy");
                                continue;
                            }
                            if (t.busy) {
                                send("error:player_busy");
                                continue;
                            }
                            t.send("invite:" + this.name);
                        } else {
                            send("error:player_not_found");
                        }
                    } else if (line.startsWith("reject:")) {
                        String challenger = line.substring(7).trim();
                        ClientHandler c = clients.get(challenger);
                        if (c != null && c != this) {
                            c.send("rejected:" + this.name);
                        }
                    } else if (line.startsWith("accept:")) {
                        String challenger = line.substring(7).trim();
                        ClientHandler c = clients.get(challenger);
                        if (c != null && c != this) {
                            // create session
                            GameSession gs = new GameSession(c, this);
                            c.session = gs;
                            this.session = gs;
                            c.opponent = this;
                            this.opponent = c;
                            c.busy = true;
                            this.busy = true;
                            broadcastOnline();
                            gs.start();
                        } else {
                            send("error:challenger_not_found");
                        }
                    } else if (line.startsWith("move:")) {
                        if (session != null)
                            session.handleMove(this, line.substring(5));
                    } else if (line.equals("play_again")) {
                        if (session != null)
                            session.requestPlayAgain(this);
                    } else if (line.equals("leave")) {
                        // player wants to return to lobby / leave session
                        if (session != null) {
                            session.handleLeave(this);
                        } else {
                            this.busy = false;
                            broadcastOnline();
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("ClientHandler " + name + " disconnected: " + e.getMessage());
            } finally {
                cleanup();
            }
        }

        void cleanup() {
            try {
                clients.remove(this.name);
                broadcastOnline();
                if (opponent != null)
                    opponent.send("opponent_left:" + this.name);
                if (socket != null)
                    socket.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    // === GameSession inner class ===
    private static class GameSession {
        private final ClientHandler p1;
        private final ClientHandler p2;
        private final char[][] board = new char[3][3];
        private volatile int currentPlayer = 1; // 1 -> p1 (X), 2 -> p2 (O)
        private volatile boolean p1Play = false, p2Play = false;
        private volatile boolean ended = false; // whether the last game finished
        private final StringBuilder moves = new StringBuilder();

        GameSession(ClientHandler a, ClientHandler b) {
            this.p1 = a;
            this.p2 = b;
            resetBoard();
            this.currentPlayer = 1;
            this.ended = false;
        }

        private void resetBoard() {
            for (int i = 0; i < 3; i++)
                for (int j = 0; j < 3; j++)
                    board[i][j] = ' ';
        }

        void start() {
            // start (or restart) a round inside this session
            this.currentPlayer = 1;
            this.ended = false;
            this.p1Play = false;
            this.p2Play = false;
            resetBoard();
            // send roles and board
            p1.send("role:X");
            p2.send("role:O");
            p1.send("opponent:" + p2.name);
            p2.send("opponent:" + p1.name);
            sendBoard();
            p1.send("your_turn");
            p2.send("wait");
        }

        synchronized void handleMove(ClientHandler from, String payload) {
            try {
                if (ended) {
                    // ignore moves after game end until a restart
                    from.send("wait");
                    return;
                }
                String[] parts = payload.split(",");
                int row = Integer.parseInt(parts[0]);
                int col = Integer.parseInt(parts[1]);
                char mark = (from == p1) ? 'X' : 'O';
                int playerId = (from == p1) ? 1 : 2;
                if (playerId != currentPlayer) {
                    from.send("wait");
                    return;
                }
                if (row < 0 || row > 2 || col < 0 || col > 2 || board[row][col] != ' ') {
                    from.send("invalid_move");
                    from.send("your_turn");
                    if (from == p1)
                        p2.send("wait");
                    else
                        p1.send("wait");
                    return;
                }
                board[row][col] = mark;
                moves.append((from == p1 ? "P1" : "P2")).append(":").append(row).append(",").append(col).append(";");
                sendBoard();
                if (checkWin(mark)) {
                    ended = true;
                    if (from == p1) {
                        p1.send("win");
                        p2.send("lose");
                        String entry = LocalDateTime.now() + " - P1 wins - " + p1.name + " vs " + p2.name + " - moves:" + moves.toString();
                        appendHistory(entry);
                        Db.storeMatchResultSafe(p1.name, p2.name, 1, moves.toString());
                    } else {
                        p2.send("win");
                        p1.send("lose");
                        String entry = LocalDateTime.now() + " - P2 wins - " + p1.name + " vs " + p2.name + " - moves:" + moves.toString();
                        appendHistory(entry);
                        Db.storeMatchResultSafe(p1.name, p2.name, 2, moves.toString());
                    }
                    // keep session present so play-again will work; do not null session here
                    return;
                }
                if (isDraw()) {
                    ended = true;
                    p1.send("draw");
                    p2.send("draw");
                    String entry = LocalDateTime.now() + " - Draw - " + p1.name + " vs " + p2.name + " - moves:" + moves.toString();
                    appendHistory(entry);
                    Db.storeMatchResultSafe(p1.name, p2.name, 0, moves.toString());
                    // keep session present so play-again will work
                    return;
                }
                // swap turns
                if (currentPlayer == 1) {
                    currentPlayer = 2;
                    p2.send("your_turn");
                    p1.send("wait");
                } else {
                    currentPlayer = 1;
                    p1.send("your_turn");
                    p2.send("wait");
                }
            } catch (Exception e) {
                from.send("invalid_move");
            }
        }

        void sendBoard() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    sb.append(board[i][j]);
                    if (j < 2)
                        sb.append("|");
                }
                if (i < 2)
                    sb.append(";");
            }
            String msg = "board:" + sb.toString();
            p1.send(msg);
            p2.send(msg);
        }

        boolean checkWin(char mark) {
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

        boolean isDraw() {
            for (int i = 0; i < 3; i++)
                for (int j = 0; j < 3; j++)
                    if (board[i][j] == ' ')
                        return false;
            return true;
        }

        synchronized void requestPlayAgain(ClientHandler who) {
            // mark who requested rematch
            if (who == p1)
                p1Play = true;
            else
                p2Play = true;

            // if both requested, restart
            if (p1Play && p2Play) {
                moves.setLength(0);
                p1Play = false;
                p2Play = false;
                // reset and start a fresh round
                resetBoard();
                currentPlayer = 1;
                ended = false;
                start();
            } else {
                // inform the other side that opponent wants rematch (optional)
                if (who == p1) p2.send("opponent_wants_rematch");
                else p1.send("opponent_wants_rematch");
            }
        }

        synchronized void handleLeave(ClientHandler who) {
            ClientHandler other = (who == p1) ? p2 : p1;
            try {
                // notify other
                if (other != null) {
                    other.send("opponent_left:" + who.name);
                    // clear other's session and opponent, and mark free
                    other.session = null;
                    other.opponent = null;
                    other.busy = false;
                }
                // clear who as well
                who.session = null;
                who.opponent = null;
                who.busy = false;
                // broadcast updates
                broadcastOnline();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    // === Simple DB helper (JDBC) ===
    static final class Db {
        private static final String ENV_URL = System.getenv("DB_URL");
        private static final String ENV_USER = System.getenv("DB_USER");
        private static final String ENV_PASS = System.getenv("DB_PASS");

        static void init() {
            if (!isConfigured()) return;
            try (Connection conn = getConnection(); Statement st = conn.createStatement()) {
                st.executeUpdate("CREATE TABLE IF NOT EXISTS users (\n" +
                        "  id INT AUTO_INCREMENT PRIMARY KEY,\n" +
                        "  username VARCHAR(100) NOT NULL UNIQUE\n" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
                st.executeUpdate("CREATE TABLE IF NOT EXISTS matches (\n" +
                        "  id INT AUTO_INCREMENT PRIMARY KEY,\n" +
                        "  p1 VARCHAR(100) NOT NULL,\n" +
                        "  p2 VARCHAR(100) NOT NULL,\n" +
                        "  result TINYINT NOT NULL, -- 1=P1 win, 2=P2 win, 0=draw\n" +
                        "  moves TEXT,\n" +
                        "  played_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
                        "  INDEX(p1), INDEX(p2)\n" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            } catch (Exception e) {
                System.out.println("DB init skipped/failed: " + e.getMessage());
            }
        }

        static boolean isConfigured() {
            return ENV_URL != null && !ENV_URL.isEmpty();
        }

        static Connection getConnection() throws SQLException {
            return DriverManager.getConnection(ENV_URL, ENV_USER, ENV_PASS);
        }

        static void ensureUser(String username) {
            if (!isConfigured()) return;
            String sql = "INSERT IGNORE INTO users(username) VALUES (?)";
            try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.executeUpdate();
            } catch (Exception e) {
                System.out.println("ensureUser failed: " + e.getMessage());
            }
        }

        static void storeMatchResultSafe(String p1, String p2, int result, String moves) {
            if (!isConfigured()) return;
            String sql = "INSERT INTO matches(p1,p2,result,moves) VALUES (?,?,?,?)";
            try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, p1);
                ps.setString(2, p2);
                ps.setInt(3, result);
                ps.setString(4, moves);
                ps.executeUpdate();
                ensureUser(p1);
                ensureUser(p2);
            } catch (Exception e) {
                System.out.println("storeMatchResult failed: " + e.getMessage());
            }
        }

        static String computeStatsForPlayerOrFallback(String name) {
            if (isConfigured()) {
                try (Connection c = getConnection()) {
                    int wins = count(c, "SELECT COUNT(*) FROM matches WHERE (p1=? AND result=1) OR (p2=? AND result=2)", name, name);
                    int losses = count(c, "SELECT COUNT(*) FROM matches WHERE (p1=? AND result=2) OR (p2=? AND result=1)", name, name);
                    int draws = count(c, "SELECT COUNT(*) FROM matches WHERE (p1=? OR p2=?) AND result=0", name, name);
                    int played = wins + losses;
                    int rate = played == 0 ? 0 : (int)Math.round((wins * 100.0) / played);
                    return "wins=" + wins + ";losses=" + losses + ";draws=" + draws + ";rate=" + rate + "%";
                } catch (Exception e) {
                    System.out.println("computeStats DB failed, fallback to file: " + e.getMessage());
                }
            }
            return computeStatsForPlayer(name);
        }

        private static int count(Connection c, String sql, String a, String b) throws SQLException {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, a);
                ps.setString(2, b);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
            return 0;
        }
    }
}
