package unogames;

import java.io.*;
import java.net.*;
import java.util.*;

public class UnoGameServer {
    private static final int PORT = 9611;
    private static final int MAX_PLAYERS = 4;
    private static Set<Integer> readyPlayers = new HashSet<>();
    private static boolean gameStarted = false;
    private static List<ClientThread> clients = Collections.synchronizedList(new ArrayList<>());
    private static GameManager gameManager = new GameManager();

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("UNO Server started on port " + PORT);

        while (clients.size() < MAX_PLAYERS) {
            Socket socket = serverSocket.accept();
            ClientThread client = new ClientThread(socket, clients.size());
            clients.add(client);
            new Thread(client).start();
            System.out.println("Player " + clients.size() + " connected.");
        }
    }

    public static void broadcastToAll(String message) {
        for (ClientThread client : clients) {
            client.sendMessage(message);
        }
    }

    static class ClientThread implements Runnable {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private int playerId;

        public ClientThread(Socket socket, int playerId) throws IOException {
            this.socket = socket;
            this.playerId = playerId;
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        }

        public void run() {
            try {
                out.println("Welcome Player " + (playerId + 1));
                while (true) {
                    String command = in.readLine();
                    if (command == null)
                        break;

                    if (!gameStarted && command.equalsIgnoreCase("ready")) {
                        synchronized (readyPlayers) {
                            if (!readyPlayers.contains(playerId)) {
                                readyPlayers.add(playerId);
                                broadcastToAll("Player " + (playerId + 1) + " is ready. (" +
                                    readyPlayers.size() + "/" + clients.size() + ")");
                            }
                            if (readyPlayers.size() == clients.size()) {
                                gameStarted = true;
                                broadcastToAll("All players are ready. Starting the game...");
                                gameManager.startGame(clients);
                            }
                        }
                        continue;
                    }

                    if (!gameStarted) {
                        out.println("Waiting for all players to type 'ready'.");
                        continue;
                    }

                    gameManager.processCommand(command, this);
                }

                System.out.println("Player " + (playerId + 1) + " disconnected.");

            } catch (IOException e) {
                System.out.println("Player " + (playerId + 1) + " disconnected.");
            } finally {
                close();
                synchronized (clients) {
                    clients.remove(this);
                }
                synchronized (readyPlayers) {
                    readyPlayers.remove(playerId);
                }
                broadcastToAll("Player " + (playerId + 1) + " has left the game.");
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        public int getPlayerId() {
            return playerId;
        }

        public void close() {
            try {
                if (in != null)
                    in.close();
                if (out != null)
                    out.close();
                if (socket != null && !socket.isClosed())
                    socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
    
}
