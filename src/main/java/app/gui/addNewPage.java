package app.gui;

import java.io.*;

import app.core.KeyDerivator;
import app.core.Vault;
import app.core.Vault.InternalException;
import app.core.KeyDerivator.InvalidPasswordException;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

public class AddNewPage extends Stage{

    private static Stage primaryStage;

    private static String vaultName = "";
    private static String storagePath = "";

    static Vault newVault = null;
    
    public static void addNewPage(){
        // Create a new stage for the new "page"
        primaryStage = new Stage();
        primaryStage.setTitle("Add New Vault");

        // Create a text field for entering the name
        Label label = new Label("Insert the name of the new vault");
        final TextField nameTextField = new TextField();
        nameTextField.setPromptText("Enter the name of the new vault");

        // Create a button for submitting the name
        HBox button = new HBox(10);
        Button submitButton = new Button("Submit");
        // Handle bottomButton click event
        submitButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if(nameTextField.getText() != ""){
                    vaultName = nameTextField.getText();
                    chooseLocation();
                } else{
                    Alert alert = new Alert(AlertType.INFORMATION, "No name entered", ButtonType.OK);
                    alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                    alert.show();
                }
            }
        });
        button.getChildren().add(submitButton);
        button.setAlignment(Pos.CENTER);

        // Create a layout and add the text field and button
        HBox layout = new HBox(10);
        layout.getChildren().addAll(label, nameTextField);
        layout.setAlignment(Pos.CENTER);

        // Create the main layout
        BorderPane borderPane = new BorderPane();
        borderPane.setCenter(layout);
        borderPane.setBottom(submitButton);


        // Set the new scene for the new stage
        Scene newPageScene = new Scene(borderPane, 500, 400);
        primaryStage.setScene(newPageScene);


        // Show the new stage
        primaryStage.show();
    }

    public static void chooseLocation(){

        primaryStage.setTitle("Add New Vault");
        Label label = new Label("Where do you want to save the encrypted files of the new vault?");

        // Create a button for navigating back to the name input page
        Button previousPageButton = new Button("Previous");
        previousPageButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                addNewPage();
            }
        });

        final DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setInitialDirectory(new File("src"));

        Label choseLocation = new Label("The choosen storage location:");
        final Label chosenLocation = new Label("");

        Button chooseLocationButton  = new Button("Choose Location");
        chooseLocationButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                DirectoryChooser directoryChooser = new DirectoryChooser();
                directoryChooser.setTitle("Choose Storage Location");

                // Show the directory chooser dialog
                File selectedDirectory = directoryChooser.showDialog(primaryStage);

                if (selectedDirectory != null) {
                    //System.out.println("Selected Storage Location: " + selectedDirectory.getAbsolutePath());
                    storagePath = selectedDirectory.getAbsolutePath();
                    chosenLocation.setText(storagePath);
                } else {
                    System.out.println("No storage location selected.");
                    storagePath = "";
                }
            }

        });

        // Create a button for navigating back to the name input page
        Button nextPageButton = new Button("Next");
        nextPageButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if(storagePath != ""){
                    choosePassword();
                } else{
                    Alert alert = new Alert(AlertType.INFORMATION, "No storage location entered", ButtonType.OK);
                    alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                    alert.show();
                }
            }
        });

        // Create a layout and add the "Choose Storage Location" and "Previous" buttons
        VBox layout = new VBox(10);
        layout.getChildren().addAll(label, chooseLocationButton, choseLocation, chosenLocation);
        layout.setAlignment(Pos.CENTER);

        HBox buttons = new HBox();
        buttons.getChildren().addAll(previousPageButton, nextPageButton);
        buttons.setAlignment(Pos.CENTER);

        // Create the main layout
        BorderPane borderPane = new BorderPane();
        borderPane.setCenter(layout);
        borderPane.setBottom(buttons);

        // Set the new scene for the new stage
        Scene newPageScene = new Scene(borderPane, 500, 400);
        primaryStage.setScene(newPageScene);

        // Show the new stage
        primaryStage.show();
    }

    public static void choosePassword(){
        
        primaryStage.setTitle("Add New Vault");

        Label password = new Label("Enter a new password");
        final PasswordField passwordField = new PasswordField();

        Label passwordRe = new Label("Confirm the new password");
        final PasswordField passwordFieldRe = new PasswordField();

        final Label[] statusPsw = {new Label("The password must contain at least 12 characters"),
                                        new Label("The password must contain a maximum of 64 characters"),
                                        new Label("The password must contain at least one special character"),
                                        new Label("The password must contain at least one upper case character"),
                                        new Label("The password must contain at least one number"),
                                        new Label("The password must contain at least one lower case character")};

        for(int i=0; i < statusPsw.length; i++){
            statusPsw[i].setTextFill(Color.RED);
        }

        passwordField.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                KeyDerivator kd = new KeyDerivator();
                String message = "";
                try {
                    kd.validatePassword(newValue);
                } catch (InvalidPasswordException e) {
                    message = e.getMessage();
                }
                
                for(int i=0; i < statusPsw.length; i++){
                    if(!message.contains(statusPsw[i].getText()))
                        statusPsw[i].setTextFill(Color.GREEN);
                    else
                        statusPsw[i].setTextFill(Color.RED);
                }
            }
        });

        // Create a button for navigating back to the name input page
        Button previousPageButton = new Button("Previous");
        previousPageButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                chooseLocation();
            }
        });

        // Create a button for navigating back to the name input page
        Button nextPageButton = new Button("Create Vault");
        nextPageButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try{
                    KeyDerivator kd = new KeyDerivator();
                    kd.validatePassword(passwordField.getText());

                    if(passwordField.getText().equals(passwordFieldRe.getText())){
                    
                        Alert alert = new Alert(AlertType.CONFIRMATION, "Added Vault", ButtonType.OK);
                        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                        alert.show();
    
                        primaryStage.close();
    
                        try {
                            newVault = new Vault(storagePath, passwordField.getText());
                            FirstPage.addNewVault(newVault, vaultName);
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (InternalException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (InvalidPasswordException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        
                    } else{
                        Alert alert = new Alert(AlertType.INFORMATION, "Password must be equals", ButtonType.OK);
                        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                        alert.show();
                    }
                    
                } catch (InvalidPasswordException e) {
                    Alert alert = new Alert(AlertType.WARNING, e.getMessage(), ButtonType.OK);
                    alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                    alert.show();
                }
            }
        });

        // Create a layout 
        VBox layout = new VBox(10);
        layout.getChildren().addAll(password, passwordField);
        layout.getChildren().addAll(statusPsw);
        layout.setAlignment(Pos.CENTER);

        // Create a layout 
        VBox labels = new VBox(10);
        layout.getChildren().addAll(passwordRe, passwordFieldRe);
        layout.setAlignment(Pos.CENTER);

        HBox buttons = new HBox();
        buttons.getChildren().addAll(previousPageButton, nextPageButton);
        buttons.setAlignment(Pos.CENTER);

        // Create the main layout
        BorderPane borderPane = new BorderPane();
        borderPane.setTop(layout);
        borderPane.setCenter(labels);
        borderPane.setBottom(buttons);

        // Set the new scene for the new stage
        Scene newPageScene = new Scene(borderPane, 500, 400);
        primaryStage.setScene(newPageScene);

        // Show the new stage
        primaryStage.show();
    }
}
