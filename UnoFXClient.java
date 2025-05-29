package unogames;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.util.*;

public class UnoFXClient extends Application {

    private TextArea messageArea = new TextArea();
    private HBox handBox = new HBox(10);
    private Label topCardLabel = new Label("Top card: ");
    private Button drawButton = new Button("Draw");
    private Button readyButton = new Button("Ready");

    private Button restartButton = new Button("Restart");
    private Button exitButton = new Button("Exit");

    private PrintWriter out;
    private BufferedReader in;

    private List<String> myHand = new ArrayList<>();
    private String currentTopCard = "";

    private HBox controls = new HBox(10); // แยกไว้ใช้ toggle ปุ่มภายหลัง

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();

        messageArea.setEditable(false);
        messageArea.setPrefHeight(200);

        handBox.setPadding(new Insets(10));
        handBox.setAlignment(Pos.CENTER);

        // topCard
        topCardLabel.setStyle(
                "-fx-border-color: black; -fx-padding: 10; -fx-background-color: lightyellow; -fx-font-weight: bold;");
        VBox topBox = new VBox(10, new Label("Top Card:"), topCardLabel);
        topBox.setAlignment(Pos.CENTER);
        topBox.setPadding(new Insets(10));

        Separator separator = new Separator();
        VBox topSection = new VBox(10, topBox, separator);
        topSection.setAlignment(Pos.CENTER);

        // Controls
        controls.getChildren().addAll(drawButton, readyButton);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(10));

        root.setTop(topSection);
        root.setCenter(handBox);
        root.setBottom(new VBox(10, messageArea, controls));

        Scene scene = new Scene(root, 700, 450);
        primaryStage.setScene(scene);
        primaryStage.setTitle("UNO JavaFX Client");
        primaryStage.show();

        connectToServer();

        // Button actions
        drawButton.setOnAction(e -> sendCommand("draw"));

        readyButton.setOnAction(e -> {
            sendCommand("ready");
            readyButton.setDisable(true);
        });

        // Restart & Exit (เริ่มต้นยังไม่แสดง)
        restartButton.setOnAction(e -> {
            sendCommand("restart");
            restartButton.setDisable(true);
            exitButton.setDisable(true);
            messageArea.appendText("Requesting game restart...\n");
        });

        exitButton.setOnAction(e -> Platform.exit());
    }

    private void connectToServer() {
        new Thread(() -> {
            try (Socket socket = new Socket("localhost", 9611)) {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                String line;
                while ((line = in.readLine()) != null) {
                    String message = line;
                    Platform.runLater(() -> processMessage(message));
                }

                // เมื่อ readLine() return null = server ปิดการเชื่อมต่อ
                Platform.runLater(() -> messageArea.appendText("Disconnected from server.\n"));

            } catch (IOException e) {
                Platform.runLater(() -> messageArea.appendText("Disconnected from server (error).\n"));
            }
        }).start();
    }

    private void processMessage(String message) {
        System.out.println("[SERVER MESSAGE] " + message);
        messageArea.appendText(message + "\n");

        if (message.startsWith("Your cards: ")) {
            String cardsStr = message.substring("Your cards: ".length());
            myHand = Arrays.asList(cardsStr.split(" "));
            updateHandDisplay();
        }

        if (message.contains("Top card: ")) {
            int idx = message.indexOf("Top card: ");
            currentTopCard = message.substring(idx + "Top card: ".length()).trim().split(" ")[0];
            updateTopCardDisplay();
        }

        if (message.startsWith("Game started!") && message.contains("Top card: ")) {
            int idx = message.indexOf("Top card: ");
            currentTopCard = message.substring(idx + "Top card: ".length()).trim().split(" ")[0];
            updateTopCardDisplay();
        }

        // แสดงปุ่มเมื่อเกมจบ
        String msgLower = message.toLowerCase();
        if (msgLower.contains("game over") || msgLower.contains("wins!") || msgLower.contains("game ended")) {
            showGameEndOptions();
        }

        if (msgLower.contains("game restarted")) {
            hideGameEndOptions();
        }

    }

    private void showGameEndOptions() {
        if (!controls.getChildren().contains(restartButton)) {
            controls.getChildren().addAll(restartButton, exitButton);
        }
        restartButton.setDisable(false);
        exitButton.setDisable(false);
        drawButton.setDisable(true);
        readyButton.setDisable(true);
    }

    private void hideGameEndOptions() {
        controls.getChildren().removeAll(restartButton, exitButton);
        drawButton.setDisable(false);
        readyButton.setDisable(false);
    }

    private void updateTopCardDisplay() {
        topCardLabel.setText(currentTopCard);
    }

    private void updateHandDisplay() {
        handBox.getChildren().clear();
        for (String card : myHand) {
            Button cardButton = new Button(card);
            cardButton.setStyle(
                    "-fx-border-color: black; -fx-padding: 10; -fx-background-color: white; -fx-font-weight: bold;");

            // คลิกการ์ดเพื่อเล่น
            cardButton.setOnAction(e -> handleCardClick(card));

            handBox.getChildren().add(cardButton);
        }
    }

    private void handleCardClick(String card) {
        if (card.equalsIgnoreCase("W") || card.equalsIgnoreCase("W4+")) {
            List<String> colors = Arrays.asList("R", "G", "B", "Y");
            ChoiceDialog<String> dialog = new ChoiceDialog<>("R", colors);
            dialog.setTitle("Choose Color");
            dialog.setHeaderText("Select color for Wild card");
            dialog.setContentText("Color:");

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(color -> sendCommand("play " + card + " " + color));
        } else {
            sendCommand("play " + card);
        }
    }

    private void sendCommand(String cmd) {
        if (out != null) {
            out.println(cmd);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}