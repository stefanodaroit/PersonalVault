package app.gui;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class firstPage extends Application {

  public final int SCENE_WIDTH = 600;
  public final int SCENE_HEIGHT = 400;

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
    BorderPane borderPane = new BorderPane();
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
          // Create a new stage for the new "page"
          final Stage newStage = new Stage();

          // Create content for the new page
          VBox newPageLayout = new VBox();
          newPageLayout.setSpacing(10);
          newPageLayout.setAlignment(Pos.CENTER);

          Label newPageLabel = new Label("New Page Content");
          Button closeButton = new Button("Close");

          // Close the new page on button click
          closeButton.setOnAction(new EventHandler<ActionEvent>() {
              @Override
              public void handle(ActionEvent closeEvent) {
                  newStage.close();
              }
          });

          // Add content to the new page layout
          newPageLayout.getChildren().addAll(newPageLabel, closeButton);

          // Set the new scene for the new stage
          Scene newPageScene = new Scene(newPageLayout, 300, 200);
          newStage.setScene(newPageScene);

          // Show the new stage
          newStage.show();
      }
  });


    primaryStage.show();
  }

}