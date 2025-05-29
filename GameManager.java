package unogames;

import java.util.*;

public class GameManager {
    private List<UnoGameServer.ClientHandler> clients;
    private List<List<String>> playerHands;
    private int currentPlayer = 0;
    private String topCard = "R0";
    private boolean isClockwise = true;
    private String currentColor = "R";
    private boolean gameStarted = false;
    private boolean gameEnded = false;

    public boolean isGameStarted() {
        return gameStarted;
    }

    private String randomNumberCard() {
        String[] colors = { "R", "G", "B", "Y" };
        Random rnd = new Random();
        int number = rnd.nextInt(10); // 0-9
        String color = colors[rnd.nextInt(colors.length)];
        return color + number;
    }

    public void startGame(List<UnoGameServer.ClientHandler> clients) {
        this.clients = clients;
        this.gameStarted = true;
<<<<<<< HEAD
=======
        this.gameEnded = false;
>>>>>>> b459ff1 (fix restart and exit button and add gitignore)
        this.playerHands = new ArrayList<>();
        for (int i = 0; i < clients.size(); i++) {
            List<String> hand = dealHand();
            playerHands.add(hand);
            clients.get(i).sendMessage("Your cards: " + String.join(" ", hand));
        }
        topCard = randomNumberCard(); // give a starter can be only numbers
        currentColor = String.valueOf(topCard.charAt(0));
        broadcast("Game started! Top card: " + topCard);
        clients.get(currentPlayer).sendMessage("Your turn!");
    }

    public void processCommand(String command, UnoGameServer.ClientHandler client) {
        int playerId = client.getPlayerId();

        if (gameEnded) {
            if (command.equalsIgnoreCase("restart")) {
                if (playerId != 0) {
                    client.sendMessage("Only Player 1 can restart the game.");
                    return;
                }
                broadcast("Player 1 restarted the game.");
                restartGame();
                return;
            } else if (command.equalsIgnoreCase("exit")) {
                client.sendMessage("Goodbye!");
                try {
                    client.close();
                } catch (Exception e) {
                    System.out.println("Error closing client " + playerId);
                }
                return;
            } else {
<<<<<<< HEAD
                client.sendMessage("Game over. Type 'restart' to play again or 'exit' to leave.");
=======
>>>>>>> b459ff1 (fix restart and exit button and add gitignore)
                return;
            }
        }

        if (playerId != currentPlayer) {
            client.sendMessage("Not your turn.");
            return;
        }

        if (command.startsWith("play ")) {
            String[] parts = command.trim().split(" ");
            if (parts.length < 2) {
                client.sendMessage("Specify cards to play.");
                return;
            }

            // Wild (W)
            if (parts[1].equalsIgnoreCase("W")) {
                if (parts.length < 3) {
                    client.sendMessage("Specify color after Wild card (R, G, B, Y).");
                    return;
                }
                String chosenColor = parts[2].toUpperCase();
                if (!Arrays.asList("R", "G", "B", "Y").contains(chosenColor)) {
                    client.sendMessage("Invalid color chosen.");
                    return;
                }
                if (!playerHands.get(playerId).contains("W")) {
                    client.sendMessage("You don't have that card.");
                    return;
                }

                playerHands.get(playerId).remove("W");
                topCard = "W";
                currentColor = chosenColor;
                broadcast("Player " + (playerId + 1) + " played Wild and changed color to " + chosenColor);
                sendHand(client);

                if (playerHands.get(playerId).isEmpty()) {
                    endGame(playerId);
                    return;
                }

                nextTurn();
                return;
            }

            // Wild +4 (W4+)
            if (parts[1].equalsIgnoreCase("W4+")) {
                if (parts.length < 3) {
                    client.sendMessage("Specify color after Wild +4 (R, G, B, Y).");
                    return;
                }
                String chosenColor = parts[2].toUpperCase();
                if (!Arrays.asList("R", "G", "B", "Y").contains(chosenColor)) {
                    client.sendMessage("Invalid color chosen.");
                    return;
                }
                if (!playerHands.get(playerId).contains("W4+")) {
                    client.sendMessage("You don't have that card.");
                    return;
                }

                playerHands.get(playerId).remove("W4+");
                topCard = "W4+";
                currentColor = chosenColor;
                broadcast("Player " + (playerId + 1) + " played Wild +4 and changed color to " + chosenColor);

                int nextPlayer = isClockwise ? (currentPlayer + 1) % clients.size()
                        : (currentPlayer - 1 + clients.size()) % clients.size();
                for (int i = 0; i < 4; i++) {
                    playerHands.get(nextPlayer).add(randomCard());
                }
                clients.get(nextPlayer).sendMessage("You drew 4 cards due to Wild +4.");
                sendHand(client);
                sendHand(clients.get(nextPlayer));

                if (playerHands.get(playerId).isEmpty()) {
                    endGame(playerId);
                    return;
                }

                skipTurn();
                return;
            }

            // ปกติ
            List<String> cardsToPlay = new ArrayList<>();
            for (int i = 1; i < parts.length; i++) {
                cardsToPlay.add(parts[i].toUpperCase());
            }

            if (!playerHands.get(playerId).containsAll(cardsToPlay)) {
                client.sendMessage("You don't have all the specified cards.");
                return;
            }

            String firstCard = cardsToPlay.get(0);
            String cardValue = firstCard.substring(1);

            for (String card : cardsToPlay) {
                if (card.length() < 2 || !card.substring(1).equals(cardValue)) {
                    client.sendMessage("All cards must have the same number or symbol.");
                    return;
                }
            }

            if (!isPlayable(firstCard)) {
                client.sendMessage("First card is not playable.");
                return;
            }

            playerHands.get(playerId).removeAll(cardsToPlay);
            topCard = firstCard;
            currentColor = String.valueOf(firstCard.charAt(0));
            broadcast("Player " + (playerId + 1) + " played: " + String.join(" ", cardsToPlay) + ". Top card: "
                    + topCard);
            sendHand(client);

            if (playerHands.get(playerId).isEmpty()) {
                endGame(playerId);
                return;
            }

            // การ์ดพิเศษ
            if (cardsToPlay.size() == 1) {
                switch (cardValue) {
                    case "S":
                        broadcast("Player " + ((currentPlayer + 1) % clients.size() + 1) + " is skipped!");
                        skipTurn();
                        return;
                    case "R":
                        isClockwise = !isClockwise;
                        broadcast("Play direction reversed!");
                        if (clients.size() == 2) {
                            skipTurn();
                        } else {
                            nextTurn();
                        }
                        return;
                    case "2+":
                        int drawPlayer = isClockwise ? (currentPlayer + 1) % clients.size()
                                : (currentPlayer - 1 + clients.size()) % clients.size();
                        for (int i = 0; i < 2; i++) {
                            playerHands.get(drawPlayer).add(randomCard());
                        }
                        clients.get(drawPlayer).sendMessage("You drew 2 cards due to +2.");
                        sendHand(clients.get(drawPlayer));
                        skipTurn();
                        return;
                    default:
                        nextTurn();
                        return;
                }
            } else {
                nextTurn();
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
        currentPlayer = isClockwise ? (currentPlayer + 1) % clients.size()
                : (currentPlayer - 1 + clients.size()) % clients.size();
        clients.get(currentPlayer).sendMessage("Your turn!");
    }

    private void skipTurn() {
        currentPlayer = isClockwise ? (currentPlayer + 2) % clients.size()
                : (currentPlayer - 2 + clients.size()) % clients.size();
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
        String[] colors = { "R", "G", "B", "Y" };
        Random rnd = new Random();
        int type = rnd.nextInt(15);
        if (type < 10) {
            return colors[rnd.nextInt(colors.length)] + type;
        } else if (type == 10) {
            return colors[rnd.nextInt(colors.length)] + "S";
        } else if (type == 11) {
            return colors[rnd.nextInt(colors.length)] + "R";
        } else if (type == 12) {
            return colors[rnd.nextInt(colors.length)] + "2+";
        } else if (type == 13) {
            return "W";
        } else {
            return "W4+";
        }
    }

    private boolean isPlayable(String card) {
        if (card.equals("W") || card.equals("W4+"))
            return true;
        String cardColor = card.substring(0, 1);
        String cardValue = card.substring(1);
        String topCardValue = topCard.equals("W") || topCard.equals("W4+") ? "" : topCard.substring(1);
        String topCardColor = currentColor;
        return cardColor.equals(topCardColor) || cardValue.equals(topCardValue);
    }

    private void endGame(int winnerId) {
        gameEnded = true;
        broadcast("Player " + (winnerId + 1) + " wins!");
<<<<<<< HEAD
=======
        broadcast("Game ended. Type 'restart' to play again or 'exit' to leave.");
>>>>>>> b459ff1 (fix restart and exit button and add gitignore)
        for (UnoGameServer.ClientHandler client : clients) {
            if (client.getPlayerId() != winnerId) {
                client.sendMessage("You lose.");
            }
<<<<<<< HEAD
            client.sendMessage("Game ended. Type 'restart' to play again or 'exit' to leave.");
=======
>>>>>>> b459ff1 (fix restart and exit button and add gitignore)
        }
    }

    private void restartGame() {
        this.currentPlayer = 0;
        this.topCard = randomNumberCard();
        this.currentColor = String.valueOf(topCard.charAt(0));
        this.isClockwise = true;
        this.gameEnded = false;
        this.playerHands.clear();

        for (int i = 0; i < clients.size(); i++) {
            List<String> hand = dealHand();
            playerHands.add(hand);
            clients.get(i).sendMessage("Your cards: " + String.join(" ", hand));
        }

        broadcast("Game restarted! Top card: " + topCard);
        clients.get(currentPlayer).sendMessage("Your turn!");
    }
}
