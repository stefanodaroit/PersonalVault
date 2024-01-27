package app.gui;

import app.core.KeyDerivator;
import app.core.KeyDerivator.InvalidPasswordException;
import app.core.Vault;
import app.core.Vault.InternalException;
import app.core.Vault.InvalidConfigurationException;
import app.core.Vault.WrongPasswordException;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
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
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.*;


public class FirstPage extends Application {

  public final static int SCENE_WIDTH = 1800;
  public final int SCENE_HEIGHT = 900;

  static BorderPane borderPane = new BorderPane();
  static HashMap<Vault, Button> newVaults = new HashMap<Vault, Button>();
  static VBox rightPart = new VBox();

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

  public static void addNewVault(final Vault newVault){

    Button vault = new Button(newVault.getName() + "\n" + newVault.getStoragePath());
    vault.setWrapText(true);
    vault.setMaxWidth(SCENE_WIDTH * 0.4);
    
    newVaults.put(newVault, vault);

    VBox vaultButtons = new VBox();

    for(final Vault v: newVaults.keySet()){
      vaultButtons.getChildren().addAll(newVaults.get(v));
    }

    addUnlockOptions();
    
    borderPane.setLeft(vaultButtons);
  }

  private static void addUnlockOptions(){

    for(final Vault v: newVaults.keySet()){
      
      // Handle bottomButton click event without lambda expression
      newVaults.get(v).setOnAction(new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
          rightPart = new VBox();

          Label label = new Label(v.getName() + "\n" + v.getStoragePath());
          Button unlockButton = new Button("Unlock");
          Button changePswButton = new Button("Change Password");

          unlockButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
              unlockVault(v);     
            }
          });

          changePswButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
              changePsw(v);     
            }
          });
          
          rightPart.getChildren().addAll(label, unlockButton, changePswButton);
          rightPart.setAlignment(Pos.TOP_LEFT);
          borderPane.setRight(rightPart);
        }
      }); 
    }
    
  }

  private static void changePsw(final Vault v){
    final Stage primaryStage = new Stage();
    primaryStage.setTitle("Change Password");

    Label oldPassword = new Label("Enter the current password for " + v.getName());
    final PasswordField oldPasswordField = new PasswordField();

    Label newPassword = new Label("Enter a new password");
    final PasswordField newPasswordField = new PasswordField();

    Label newPasswordRe = new Label("Confirm the new password");
    final PasswordField newPasswordFieldRe = new PasswordField();

    final Label[] statusPsw = {new Label("The password must contain at least 12 characters"),
                                    new Label("The password must contain a maximum of 64 characters"),
                                    new Label("The password must contain at least one special character"),
                                    new Label("The password must contain at least one upper case character"),
                                    new Label("The password must contain at least one number"),
                                    new Label("The password must contain at least one lower case character")};

    for(int i=0; i < statusPsw.length; i++){
        statusPsw[i].setTextFill(Color.RED);
    }

    newPasswordField.textProperty().addListener(new ChangeListener<String>() {
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
    Button cancelPageButton = new Button("Cancel");
    cancelPageButton.setOnAction(new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
            primaryStage.close();
        }
    });

    // Create a button for navigating back to the name input page
    Button changePswButton = new Button("Change");
    changePswButton.setOnAction(new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
            if(newPasswordField.getText().equals(newPasswordFieldRe.getText())){
              try{
                v.changePsw(oldPasswordField.getText(), newPasswordField.getText());
                Alert alert = new Alert(AlertType.CONFIRMATION, "The new password is setted", ButtonType.OK);
                alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                alert.show();
                primaryStage.close();
              } catch (InvalidPasswordException e){
                Alert alert = new Alert(AlertType.ERROR, e.getMessage(), ButtonType.OK);
                alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                alert.show();
              } catch (WrongPasswordException e) {
                Alert alert = new Alert(AlertType.ERROR, e.getMessage(), ButtonType.OK);
                alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                alert.show();
              } catch (InternalException e) {
                // TODO Auto-generated catch block
                e.getMessage();
              } catch (IOException e) {
                // TODO Auto-generated catch block
                e.getMessage();
              }
              
            } else {
              Alert alert = new Alert(AlertType.ERROR, "The two passwords must be equal", ButtonType.OK);
              alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
              alert.show();
            }
            
        }
    });

    // Create a layout 
    VBox layout = new VBox(10);
    layout.getChildren().addAll(oldPassword, oldPasswordField, newPassword, newPasswordField);
    layout.getChildren().addAll(newPasswordRe, newPasswordFieldRe);
    layout.getChildren().addAll(statusPsw);
    layout.setAlignment(Pos.CENTER);

    HBox buttons = new HBox();
    buttons.getChildren().addAll(cancelPageButton, changePswButton);

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

  private static void unlockVault(final Vault v){
    final Stage primaryStage = new Stage();
    primaryStage.setTitle("Unlock Vault");

    Label password = new Label("Enter password for " + v.getName());
    final PasswordField passwordField = new PasswordField();

    // Create a button for cancel input page
    Button cancelPageButton = new Button("Cancel");
    cancelPageButton.setOnAction(new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
            primaryStage.close();
        }
    });

    // Create a button for cancel input page
    Button unlockPageButton = new Button("Unlock");
    unlockPageButton.setOnAction(new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
          try {
            v.unlock(passwordField.getText());
            Alert alert = new Alert(AlertType.CONFIRMATION, "Unlocked successfully!", ButtonType.OK);
            alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
            alert.show();
            primaryStage.close();
            revealDrive(v);
          } catch (InvalidConfigurationException e) {
            // TODO Auto-generated catch block
            e.getMessage();
          } catch (WrongPasswordException e) {
            Alert alert = new Alert(AlertType.WARNING, e.getMessage(), ButtonType.OK);
            alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
            alert.show();
          } catch (InternalException e) {
            e.getMessage();
          }
        }
    });

    // Create a layout 
    VBox layout = new VBox(10);
    layout.getChildren().addAll(password, passwordField);
    layout.setAlignment(Pos.CENTER);

    // Create a layout 
    HBox buttons = new HBox(10);
    buttons.getChildren().addAll(cancelPageButton, unlockPageButton);
    buttons.setAlignment(Pos.CENTER);


    // Create the main layout
    BorderPane borderPaneNew = new BorderPane();
    borderPaneNew.setTop(layout);
    borderPaneNew.setBottom(buttons);

    // Set the new scene for the new stage
    Scene newPageScene = new Scene(borderPaneNew, 500, 400);
    primaryStage.setScene(newPageScene);

    // Show the new stage
    primaryStage.show();
  }

  private static void revealDrive(final Vault v){
    Label label = new Label(v.getName() + "\n" + v.getStoragePath());
    Label revealLabel = new Label("Your vault's contants are accessible here:");
    Button revealButton = new Button("Reveal Vault");
    Button lockButton = new Button("Lock");

    revealButton.setOnAction(new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent event) {
        //TODO
  
      }
    });

    lockButton.setOnAction(new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent event) {
        v.lock();
        rightPart = new VBox();
        borderPane.setRight(rightPart);
      }
    });

    rightPart = new VBox();
    rightPart.getChildren().addAll(label, revealLabel, revealButton, lockButton);
    rightPart.setAlignment(Pos.TOP_LEFT);
    

    borderPane.setRight(rightPart);
    

  }


}