package unogames;

import java.util.*;

public class GameManager {
    private List<UnoGameServer.ClientHandler> clients;
    private List<List<String>> playerHands;
    private int currentPlayer = 0;
    private String topCard = "R0";
    private boolean isClockwise = true;
    private String currentColor = "R";  // wild

    public void startGame(List<UnoGameServer.ClientHandler> clients) {
        this.clients = clients;
        this.playerHands = new ArrayList<>();
        for (int i = 0; i < clients.size(); i++) {
            List<String> hand = dealHand();
            playerHands.add(hand);
            clients.get(i).sendMessage("Your cards: " + String.join(" ", hand));
        }
        topCard = randomCard();
        currentColor = String.valueOf(topCard.charAt(0));
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
            // "play <card>" or "play W <color>" 
            String[] parts = command.split(" ");
            String card = parts[1].trim();

            if (!playerHands.get(playerId).contains(card)) {
                client.sendMessage("You don't have that card.");
                return;
            }

            if (card.equals("W")) {
                // Wild card : select color
                if (parts.length < 3) {
                    client.sendMessage("Specify color after Wild card (R, G, B, Y). Example: play W R");
                    return;
                }
                String chosenColor = parts[2].toUpperCase();
                if (!Arrays.asList("R", "G", "B", "Y").contains(chosenColor)) {
                    client.sendMessage("Invalid color chosen. Choose R, G, B or Y.");
                    return;
                }
                // Wild
                playerHands.get(playerId).remove(card);
                topCard = "W";  // Wild card
                currentColor = chosenColor;
                broadcast("Player " + (playerId + 1) + " played Wild and changed color to " + chosenColor);
                sendHand(client);
                if (playerHands.get(playerId).isEmpty()) {
                    broadcast("Player " + (playerId + 1) + " wins!");
                    return;
                }
                nextTurn();
                return;
            }

            // is card playable (Color/Number/Skip/Reverse)
            if (isPlayable(card)) {
                playerHands.get(playerId).remove(card);
                topCard = card;
                currentColor = String.valueOf(card.charAt(0));
                broadcast("Player " + (playerId + 1) + " played " + card + ". Top card: " + topCard);
                sendHand(client);

                // card empty
                if (playerHands.get(playerId).isEmpty()) {
                    broadcast("Player " + (playerId + 1) + " wins!");
                    return;
                }

                // special card
                char cardType = card.charAt(1);
                switch (cardType) {
                    case 'S':  // Skip
                        broadcast("Player " + ((currentPlayer + 1) % clients.size() + 1) + " is skipped!");
                        skipTurn();
                        break;
                    case 'R':  // Reverse
                        isClockwise = !isClockwise;
                        broadcast("Play direction reversed!");
                        if (clients.size() == 2) {
                            // 2 player as a skip
                            skipTurn();
                        } else {
                            nextTurn();
                        }
                        break;
                    default:
                        nextTurn();
                        break;
                }
            } else {
                client.sendMessage("Invalid card or color/number mismatch.");
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

    private void sendHand(UnoGameServer.ClientHandler client) {
        int pid = client.getPlayerId();
        List<String> hand = playerHands.get(pid);
        client.sendMessage("Your cards: " + String.join(" ", hand));
    }

    private void nextTurn() {
        if (isClockwise) {
            currentPlayer = (currentPlayer + 1) % clients.size();
        } else {
            currentPlayer = (currentPlayer - 1 + clients.size()) % clients.size();
        }
        clients.get(currentPlayer).sendMessage("Your turn!");
    }

    private void skipTurn() {
        // skip
        if (isClockwise) {
            currentPlayer = (currentPlayer + 2) % clients.size();
        } else {
            currentPlayer = (currentPlayer - 2 + clients.size()) % clients.size();
        }
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
        String[] specials = {"S", "R"};  // Skip, Reverse
        Random rnd = new Random();
        String color = colors[rnd.nextInt(colors.length)];

        int type = rnd.nextInt(12);
        if (type < 10) {
            // 0-9
            return color + type;
        } else if (type == 10) {
            // Skip
            return color + "S";
        } else {
            // Reverse
            return color + "R";
        }
        // Wild not a randomCard because it's a special card
    }

    private boolean isPlayable(String card) {
        // Wild can play all the time
        if (card.equals("W")) return true;

        char cardColor = card.charAt(0);
        char cardValue = card.charAt(1);

        // Card on the table
        char topColor = currentColor.charAt(0);
        char topValue = (topCard.equals("W")) ? ' ' : topCard.charAt(1);

        // Can play if
        // 1) The color matches currentColor.
        // 2) Number/special character matches topCard (for example, R7 can be played if the topCard is G7)
        // 3) Special cards Skip or Reverse can be used if they are the same (color or number matching condition 1 or 2)
        if (cardColor == topColor) return true;
        if (cardValue == topValue) return true;

        return false;
    }
}
