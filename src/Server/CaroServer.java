package Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class CaroServer {
    private static final int PORT = 8000;
    private static Socket player1Socket;
    private static Socket player2Socket;
    private static PrintWriter out1, out2;
    private static BufferedReader in1, in2;
    private static char[][] board = new char[3][3];

    private static BlockingQueue<String> moveQueue = new LinkedBlockingQueue<>();
    private static volatile boolean running = true;
    private static volatile boolean p1PlayAgain = false;
    private static volatile boolean p2PlayAgain = false;
    private static StringBuilder currentGameMoves = new StringBuilder();
    private static String p1Name = "P1";
    private static String p2Name = "P2";

    public static void main(String[] args) {
        // Server starting (silent)
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            // waiting for player 1
            player1Socket = serverSocket.accept();
            out1 = new PrintWriter(player1Socket.getOutputStream(), true);
            in1 = new BufferedReader(new InputStreamReader(player1Socket.getInputStream()));
            out1.println("role:X");
            // player 1 connected

            // waiting for player 2
            player2Socket = serverSocket.accept();
            out2 = new PrintWriter(player2Socket.getOutputStream(), true);
            in2 = new BufferedReader(new InputStreamReader(player2Socket.getInputStream()));
            out2.println("role:O");
            // player 2 connected

            // start reader threads
            Thread r1 = new Thread(() -> readerLoop(in1, 1), "reader-1");
            Thread r2 = new Thread(() -> readerLoop(in2, 2), "reader-2");
            r1.start();
            r2.start();
            // manage multiple games
            while (running) {
                // clear moves for new game
                currentGameMoves.setLength(0);
                // keep player names between games
                gameLoop();

                // after a game ends, wait for both players to request play again
                // Game ended. Waiting for play_again from both players...
                p1PlayAgain = false;
                p2PlayAgain = false;
                while (running && !(p1PlayAgain && p2PlayAgain)) {
                    Thread.sleep(200);
                }
                if (!running)
                    break;
                // Both players requested play again. Starting new game.
            }

            // shutdown
            running = false;
            r1.interrupt();
            r2.interrupt();
            closeAll();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException ie) {
            // Server main loop interrupted
        }
    }

    private static void readerLoop(BufferedReader in, int playerId) {
        try {
            String line;
            while (running && (line = in.readLine()) != null) {
                // received from player (silent)
                if (line.startsWith("login:")) {
                    String name = line.substring(6).trim();
                    if (playerId == 1)
                        p1Name = name.isEmpty() ? "P1" : name;
                    else
                        p2Name = name.isEmpty() ? "P2" : name;
                    // if both names known, notify each of opponent name
                    if (p1Name != null && p2Name != null) {
                        if (out1 != null)
                            out1.println("opponent:" + p2Name);
                        if (out2 != null)
                            out2.println("opponent:" + p1Name);
                    }
                } else if (line.startsWith("move:")) {
                    // record move for history (store row,col)
                    currentGameMoves.append("P").append(playerId).append(":").append(line.substring(5)).append(";");
                    moveQueue.put(playerId + ":" + line);
                } else if (line.equals("play_again")) {
                    if (playerId == 1)
                        p1PlayAgain = true;
                    else
                        p2PlayAgain = true;
                    // player requested play again
                } else if (line.equals("get_stats")) {
                    // compute stats for this player and send back
                    String name = (playerId == 1) ? p1Name : p2Name;
                    String stats = computeStatsForPlayer(name);
                    if (playerId == 1)
                        sendToP1("stats:" + stats);
                    else
                        sendToP2("stats:" + stats);
                }
            }
        } catch (IOException | InterruptedException e) {
            System.out.println("Reader for P" + playerId + " exiting: " + e.getMessage());
        }
    }

    private static void gameLoop() {
        resetBoard();
        sendBoard();
        // X starts
        sendToP1("your_turn");
        sendToP2("wait");
        int currentPlayer = 1; // 1 -> X; 2 -> O

        while (running) {
            try {
                String item = moveQueue.take(); // blocks
                // item format: "1:move:row,col"
                String[] parts = item.split(":", 3);
                int playerId = Integer.parseInt(parts[0]);
                String payload = parts[1] + ":" + parts[2]; // "move:row,col"
                if (playerId != currentPlayer) {
                    // Ignoring move from wrong player
                    continue;
                }
                if (payload.startsWith("move:")) {
                    String coords = payload.substring(5);
                    char mark = (playerId == 1) ? 'X' : 'O';
                    boolean ok = processMove(coords, mark);
                    if (!ok) {
                        // Invalid move from player
                        // notify player and give them back the turn
                        if (playerId == 1) {
                            sendToP1("invalid_move");
                            sendToP1("your_turn");
                            sendToP2("wait");
                        } else {
                            sendToP2("invalid_move");
                            sendToP2("your_turn");
                            sendToP1("wait");
                        }
                        // Returned turn to player after invalid move
                        continue;
                    }
                    sendBoard();
                    if (checkWin(mark)) {
                        if (playerId == 1) {
                            sendToP1("win");
                            sendToP2("lose");
                            appendHistory("P1 wins");
                        } else {
                            sendToP2("win");
                            sendToP1("lose");
                            appendHistory("P2 wins");
                        }
                        break;
                    } else if (isDraw()) {
                        sendToP1("draw");
                        sendToP2("draw");
                        appendHistory("Draw");
                        break;
                    } else {
                        // swap turn
                        if (currentPlayer == 1) {
                            currentPlayer = 2;
                            sendToP2("your_turn");
                            sendToP1("wait");
                            // switched to player 2
                        } else {
                            currentPlayer = 1;
                            sendToP1("your_turn");
                            sendToP2("wait");
                            // switched to player 1
                        }
                    }
                }
            } catch (InterruptedException e) {
                // Game loop interrupted
                break;
            }
        }
    }

    private static void resetBoard() {
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                board[i][j] = ' ';
    }

    private static void sendBoard() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                sb.append(board[i][j]);
                if (j < 2)
                    sb.append("|");
            }
            if (i < 2)
                sb.append(";"); // use ';' to separate rows in a single-line board message
        }
        String boardStr = "board:" + sb.toString();
        sendToP1(boardStr);
        sendToP2(boardStr);
        // print a human-friendly board for server logs (replace ';' with newline)
        // Sent board to both players (silent)
    }

    private static void sendToP1(String msg) {
        if (out1 != null) {
            out1.println(msg);
            // sent to P1: (silent)
        }
    }

    private static void sendToP2(String msg) {
        if (out2 != null) {
            out2.println(msg);
            // sent to P2: (silent)
        }
    }

    private static boolean processMove(String move, char mark) {
        String[] parts = move.split(",");
        int row = Integer.parseInt(parts[0]);
        int col = Integer.parseInt(parts[1]);
        if (row < 0 || row > 2 || col < 0 || col > 2 || board[row][col] != ' ')
            return false;
        board[row][col] = mark;
        // processed move
        return true;
    }

    private static boolean checkWin(char mark) {
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

    private static boolean isDraw() {
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                if (board[i][j] == ' ')
                    return false;
        return true;
    }

    private static void closeAll() {
        try {
            if (out1 != null)
                out1.close();
            if (out2 != null)
                out2.close();
            if (in1 != null)
                in1.close();
            if (in2 != null)
                in2.close();
            if (player1Socket != null)
                player1Socket.close();
            if (player2Socket != null)
                player2Socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static synchronized void appendHistory(String result) {
        try (java.io.FileWriter fw = new java.io.FileWriter("game_history.txt", true)) {
            String entry = java.time.LocalDateTime.now().toString() + " - " + result + " - " + p1Name + " vs " + p2Name
                    + " - moves:"
                    + currentGameMoves.toString() + System.lineSeparator();
            fw.write(entry);
            System.out.println("Appended history: " + entry);
        } catch (IOException e) {
            System.out.println("Failed to append history: " + e.getMessage());
        }
    }

    // Read game_history.txt and compute wins/losses/draws and win rate for given
    // player name
    private static String computeStatsForPlayer(String name) {
        int wins = 0, losses = 0, draws = 0;
        java.io.File f = new java.io.File("game_history.txt");
        if (!f.exists()) {
            return "wins=0;losses=0;draws=0;rate=0%";
        }
        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                // expected format: timestamp - RESULT - p1Name vs p2Name - moves:...
                String[] parts = line.split(" - ", 4);
                if (parts.length < 3)
                    continue;
                String result = parts[1].trim();
                String names = parts[2].trim();
                String[] two = names.split(" vs ", 2);
                String g1 = two.length > 0 ? two[0].trim() : "";
                String g2 = two.length > 1 ? two[1].trim() : "";
                if (result.equals("Draw") || result.equalsIgnoreCase("Draw")) {
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
                    // fallback: try to match result containing player name
                    if (result.contains(name) && result.toLowerCase().contains("wins"))
                        wins++;
                }
            }
        } catch (IOException e) {
            return "wins=0;losses=0;draws=0;rate=0%";
        }
        int played = wins + losses; // exclude draws for win rate
        int rate = played == 0 ? 0 : (int) Math.round((wins * 100.0) / played);
        return "wins=" + wins + ";losses=" + losses + ";draws=" + draws + ";rate=" + rate + "%";
    }
}