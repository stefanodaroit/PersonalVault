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
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;

public class firstPage extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Create buttons
        Button settingsButton = new Button("Settings");
        Button submitButton = new Button("Add");

        // Create top layout with "Personal Vault" label and buttons
        HBox topPanel = new HBox(10);
        topPanel.setPadding(new Insets(10));
        topPanel.setBackground(new Background(new BackgroundFill(Color.LIGHTBLUE, CornerRadii.EMPTY, Insets.EMPTY)));
        topPanel.setPadding(new Insets(10));
        topPanel.setSpacing(100);
        topPanel.getChildren().addAll(
                new javafx.scene.text.Text("Personal Vault")
        );

        topPanel.getChildren().add(settingsButton);

        // Create left layout with bottom button
        VBox leftPanel = new VBox(10);
        leftPanel.setAlignment(Pos.CENTER_LEFT);
        
        ChoiceBox<String> addChoice = new ChoiceBox<>();
        addChoice.getItems().addAll("New Vault", "Existing Vault");        
        addChoice.setValue("Add");
        System.out.println(addChoice.getValue());
        // Event handler for the bottomButton
        /*addButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                dynamicContent.getChildren().clear();
                // Add two buttons dynamically when bottomButton is clicked
                final ChoiceBox<String> choiceBox = new ChoiceBox<>();
                choiceBox.setValue("Option 1"); // Set a default value
                String selectedOption = choiceBox.getValue();
                leftPanel.getChildren().addAll(choiceBox);
            }
        });*/
        leftPanel.getChildren().addAll(addChoice, submitButton);
        

        // Create right layout with some content
        VBox rightPanel = new VBox(10);
        rightPanel.setAlignment(Pos.CENTER_RIGHT);

        leftPanel.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID,
                    CornerRadii.EMPTY,new BorderWidths(1))));
        rightPanel.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID,
                    CornerRadii.EMPTY,new BorderWidths(1))));

        // Create the main layout
        BorderPane borderPane = new BorderPane();
        borderPane.setTop(topPanel);
        borderPane.setLeft(leftPanel);
        borderPane.setRight(rightPanel);

        // Create the scene
        Scene scene = new Scene(borderPane, 400, 300);

        // Set up the stage
        primaryStage.setTitle("Personal Vault App");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

}