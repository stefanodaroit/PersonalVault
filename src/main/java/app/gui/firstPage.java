package app.gui;

import app.core.Vault;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;


import java.util.*;



public class FirstPage extends Application {

  public final static int SCENE_WIDTH = 1800;
  public final int SCENE_HEIGHT = 900;

  static BorderPane borderPane = new BorderPane();
  static List<Button> newVaults = new ArrayList<>();

  @Override
  public void start(Stage primaryStage) {

    // Create buttons
    Button settingsButton = new Button("Settings");
    Button addNewVaultButton = new Button("Add New Vault");
    Button addExVaultButton = new Button("Add Existing Vault");

    Label title = new Label("PERSONAL VAULT");

    // Create top layout with "Personal Vault" label and buttons
    HBox topPanel = new HBox();
    topPanel.setPadding(new Insets(10, 10, 10, 10));
    topPanel.setBackground(new Background(new BackgroundFill(Color.LIGHTBLUE, CornerRadii.EMPTY, Insets.EMPTY)));
    topPanel.getChildren().addAll(title, settingsButton);
    HBox.setHgrow(title, javafx.scene.layout.Priority.ALWAYS);
    title.setMaxWidth(Double.MAX_VALUE);


    // Left panel
    VBox leftPanel = new VBox();
    leftPanel.setPadding(new Insets(10, 10, 10, SCENE_WIDTH * 0.4));
    leftPanel.setBorder(new Border(new BorderStroke(Color.valueOf("#9E9E9E"), BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
  
  
    // Create right layout with some content
    VBox rightPanel = new VBox();
    rightPanel.setPadding(new Insets(10, 10, 10, SCENE_WIDTH * 0.6));
    rightPanel.setBorder(new Border(new BorderStroke(Color.valueOf("#9E9E9E"), BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));

    // Create the bottom panel
    HBox bottomPanel = new HBox();
    bottomPanel.setPadding(new Insets(10, 10, 10, 10));
    bottomPanel.setSpacing(10);
    bottomPanel.setAlignment(Pos.CENTER);

    // Add content to the bottom panel
    bottomPanel.getChildren().addAll(addNewVaultButton, addExVaultButton);


    // Create the main layout
    borderPane.setTop(topPanel);
    borderPane.setLeft(leftPanel);
    borderPane.setRight(rightPanel);
    borderPane.setBottom(bottomPanel);

    // Create the scene
    Scene scene = new Scene(borderPane, SCENE_WIDTH, SCENE_HEIGHT);

    // Set up the stage
    primaryStage.setTitle("Personal Vault App");
    primaryStage.setScene(scene);

    // Handle bottomButton click event without lambda expression
    addNewVaultButton.setOnAction(new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent event) {
          AddNewPage.addNewPage();
      }
    });

    primaryStage.show();

    Rectangle2D primScreenBounds = Screen.getPrimary().getVisualBounds();
    primaryStage.setX((primScreenBounds.getWidth() - primaryStage.getWidth()) / 2);
    primaryStage.setY((primScreenBounds.getHeight() - primaryStage.getHeight()) / 2);
  }

  public static void addNewVault(Vault newVault, String name){

    Button vault = new Button(name + "\n" + newVault.getStoragePath());
    vault.setWrapText(true);
    vault.setMaxWidth(SCENE_WIDTH * 0.4);

    newVaults.add(vault);

    VBox vaultButtons = new VBox(10);
    vaultButtons.getChildren().addAll(newVaults);
    
    borderPane.setLeft(vaultButtons);
  }

}