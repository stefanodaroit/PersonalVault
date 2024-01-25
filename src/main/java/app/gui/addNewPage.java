package app.gui;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class addNewPage extends Stage{
    
    public addNewPage(){
        // Create a new stage for the new "page"
        final Stage newStage = new Stage();
        newStage.setTitle("Add New Vault");
        // Create a text field for entering the name
        Label label = new Label("Insert the name of the new vault");
        TextField nameTextField = new TextField();
        nameTextField.setPromptText("Enter the name of the new vault");

        // Create a button for submitting the name
        Button submitButton = new Button("Submit");

        // Create a layout and add the text field and button
        VBox layout = new VBox(10);
        layout.getChildren().addAll(label, nameTextField, submitButton);


        // Set the new scene for the new stage
        Scene newPageScene = new Scene(layout, 300, 400);
        newStage.setScene(newPageScene);

        // Show the new stage
        newStage.show();
    }

}
