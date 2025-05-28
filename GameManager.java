package unogames;

import java.util.*;

public class GameManager {
    private List<UnoGameServer.ClientHandler> clients;
    private List<List<String>> playerHands;
    private int currentPlayer = 0;
    private String topCard = "R0";

    public void startGame(List<UnoGameServer.ClientHandler> clients) {
        this.clients = clients;
        this.playerHands = new ArrayList<>();
        for (int i = 0; i < clients.size(); i++) {
            List<String> hand = dealHand();
            playerHands.add(hand);
            clients.get(i).sendMessage("Your cards: " + String.join(" ", hand));
        }
        topCard = randomCard();
        broadcast("Game started! Top card: " + topCard);
        clients.get(currentPlayer).sendMessage("Your turn!");
    }

    public void processCommand(String command, UnoGameServer.ClientHandler client) {
        int playerId = client.getPlayerId();
        if (playerId != currentPlayer) {
            client.sendMessage("Not your turn.");
            return;
        }

        if (command.startsWith("play ")) {
            String card = command.substring(5).trim();
            if (playerHands.get(playerId).contains(card) && isPlayable(card)) {
                playerHands.get(playerId).remove(card);
                topCard = card;
                broadcast("Player " + (playerId + 1) + " played " + card + ". Top card: " + topCard);
                sendHand(client);
                if (playerHands.get(playerId).isEmpty()) {
                    broadcast("Player " + (playerId + 1) + " wins!");
                    return;
                }
                nextTurn();
            } else {
                client.sendMessage("Invalid card: must match color or number/symbol.");
            }
        } else if (command.equals("draw")) {
            String newCard = randomCard();
            playerHands.get(playerId).add(newCard);
            client.sendMessage("You drew: " + newCard);
            sendHand(client);
            nextTurn();
        } else if (command.equals("pass")) {
            client.sendMessage("You passed.");
            sendHand(client);
            nextTurn();
        } else {
            client.sendMessage("Unknown command.");
        }
    }

    private boolean isPlayable(String card) {
        String cardColor = card.substring(0, 1);
        String cardValue = card.substring(1);

        String topColor = topCard.substring(0, 1);
        String topValue = topCard.substring(1);

        return cardColor.equals(topColor) || cardValue.equals(topValue);
    }

    private void sendHand(UnoGameServer.ClientHandler client) {
        int pid = client.getPlayerId();
        List<String> hand = playerHands.get(pid);
        client.sendMessage("Your cards: " + String.join(" ", hand));
    }

    private void nextTurn() {
        currentPlayer = (currentPlayer + 1) % clients.size();
        clients.get(currentPlayer).sendMessage("Your turn!");
    }

    private void broadcast(String message) {
        for (UnoGameServer.ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    private List<String> dealHand() {
        List<String> hand = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            hand.add(randomCard());
        }
        return hand;
    }

    private String randomCard() {
        String[] colors = {"R", "G", "B", "Y"};
        int number = new Random().nextInt(10); // 0-9
        String color = colors[new Random().nextInt(colors.length)];
        return color + number;
    }
}
