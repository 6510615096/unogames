package unogames;

import java.io.*;
import java.net.*;
import java.util.*;

public class UnoGameServer {
    private static final int PORT = 12345;
    private static final int MAX_PLAYERS = 2;
    private static List<ClientHandler> clients = new ArrayList<>();
    private static GameManager gameManager = new GameManager();

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("UNO Server started on port " + PORT);

        while (clients.size() < MAX_PLAYERS) {
            Socket socket = serverSocket.accept();
            ClientHandler client = new ClientHandler(socket, clients.size());
            clients.add(client);
            new Thread(client).start();
            System.out.println("Player " + clients.size() + " connected.");
        }

        gameManager.startGame(clients);
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private int playerId;

        public ClientHandler(Socket socket, int playerId) throws IOException {
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
                    gameManager.processCommand(command, this);
                }
            } catch (IOException e) {
                System.out.println("Player " + (playerId + 1) + " disconnected.");
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        public int getPlayerId() {
            return playerId;
        }

        // 🔥 เพิ่มเมธอดนี้
        public void close() {
            try {
                socket.close();
                in.close();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
}
